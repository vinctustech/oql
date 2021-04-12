package com.vinctus.oql2

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import xyz.hyperreal.pretty._

import java.sql.ResultSet
import xyz.hyperreal.table.TextTable
import scala.annotation.tailrec
import scala.collection.immutable.VectorMap

class OQL(dm: String, val dataSource: OQLDataSource) {

  val model: DataModel =
    DMLParse(dm) match {
      case None              => sys.error("error building data model")
      case Some(m: DMLModel) => new DataModel(m, dm)
    }

  def connect: OQLConnection = dataSource.connect

  def execute[R](action: OQLConnection => R): R = {
    val conn = connect
    val res = action(conn)

    conn.close()
    res
  }

  def create(): Unit = execute(_.create(model))

  def entity(name: String): Entity = model.entities(name)

  def queryMany(oql: String, parameters: Map[String, Any] = Map()) = { //todo: async
    val query =
      OQLParse(oql) match {
        case None              => sys.error("error parsing query")
        case Some(q: OQLQuery) => q
      }

//    println(prettyPrint(query))

    def objectNode(entity: Entity, table: String, project: List[OQLProject], join: Option[(Entity, String, Attribute)]): ObjectNode = {
      val props = new mutable.LinkedHashMap[String, Node]
      val attrset = new mutable.HashSet[String]
      val subtracts = new mutable.HashSet[String]

      for (p <- project)
        p match {
          case AttributeOQLProject(label, id) =>
            entity.attributes get id.s match {
              case someAttr @ Some(attr @ Attribute(name, column, pk, required, typ)) =>
                val l = label.map(_.s).getOrElse(name)

                if (props contains l)
                  problem(label.getOrElse(id).pos, s"attribute '$l' has already been added", oql)

                props(l) = typ match {
                  case _: DataType => ExpressionNode(AttributeOQLExpression(List(Ident(name, null)), entity, table, attr))
                  case ManyToOneType(_, attr_entity) =>
                    val alias = s"$table$$${id.s}"

                    objectNode(attr_entity, alias, List(StarOQLProject), Some((entity, alias, attr)))
                  case OneToManyType(_, attr_entity, attribute) =>
                    arrayNode(OQLQuery(id, attr_entity, List(StarOQLProject), None, None, None, OQLRestrict(None, None)), someAttr)
                  case _ => ni
                }
              case None => problem(id.pos, s"unknown attribute '${id.s}'", oql)
            }
          case ExpressionOQLProject(label, expr) =>
            if (props contains label.get.s)
              problem(label.get.pos, s"attribute '${label.get.s}' has already been added", oql)

            props(label.get.s) = ExpressionNode(expr)
          case QueryOQLProject(label, query) =>
            entity.attributes get query.resource.s match {
              case Some(Attribute(name, column, pk, required, typ))
                  if !typ.isArrayType &&
                    (query.select.isDefined || query.group.isDefined || query.order.isDefined || query.restrict != OQLRestrict(None, None)) =>
                problem(query.resource.pos, s"attribute '${query.resource.s}' is not an array type", oql)
              case Some(attr @ Attribute(name, column, pk, required, ManyToOneType(entityName, attr_entity))) =>
                val l = label.map(_.s).getOrElse(name)

                if (props contains l)
                  problem(label.getOrElse(query.resource).pos, s"attribute '$l' has already been added", oql)

                val alias = s"$table$$${query.resource.s}"

                props(l) = objectNode(attr_entity, alias, query.project, Some((entity, alias, attr))) //ExpressionNode(AttributeOQLExpression(List(Ident(name, null)), entity, column))
              // case one to many
              // case many to many
              case None => problem(query.resource.pos, s"unknown attribute '${query.resource.s}'", oql)
            }
          case StarOQLProject =>
            entity.attributes.values.filter(_.typ.isDataType) foreach {
              case attr @ Attribute(name, column, pk, required, typ) =>
                props(name) = ExpressionNode(AttributeOQLExpression(List(Ident(name, null)), entity, table, attr))
            }
          case SubtractOQLProject(id) =>
            if (subtracts(id.s))
              problem(id.pos, s"attribute '${id.s}' has already been removed", oql)

            subtracts += id.s

            if (props contains id.s)
              props -= id.s
            else
              problem(id.pos, s"attribute '${id.s}' was not added with '*'", oql)
        }

      ObjectNode(props.toList, join)
    }

    def arrayNode(query: OQLQuery, join: Option[Attribute]): ArrayNode = {
      val (entity, attr) =
        join match {
          case Some(Attribute(name, column, pk, required, typ)) =>
            sys.error(s"$name, $typ")
          case None =>
            model.entities get query.resource.s match {
              case Some(e) => (e, None)
              case None    => problem(query.resource.pos, s"unknown entity '${query.resource.s}'", oql)
            }
        }

      // lookup columns for attributes

      def references(expr: OQLExpression, table: String): Unit =
        expr match {
          case InfixOQLExpression(left, _, right) =>
            references(left, table)
            references(right, table)
          case a @ AttributeOQLExpression(ids, _, _, _) =>
            entity.attributes get ids.head.s match {
              case Some(attr) =>
                a.entity = entity
                a.table = table
                a.attr = attr
              case None => problem(ids.head.pos, s"entity '${entity.name}' does not have attribute '${ids.head.s}'", oql)
            }
          case _ =>
        }

      query.select foreach (references(_, query.resource.s)) // todo: don't know if query.resource.s is right
      ArrayNode(entity, objectNode(entity, query.resource.s, query.project, None), query.select, join)
    }

    val node: Node = arrayNode(query, None)

    val sqlBuilder = new SQLQueryBuilder

    def sqlQuery(node: Node): Unit = {
      node match {
        case ArrayNode(entity, element, select, join) =>
          sqlBuilder.table(entity.table)

          if (select.isDefined)
            sqlBuilder.select(select.get)

          sqlQuery(element)
        case e @ ExpressionNode(expr) => e.idx = sqlBuilder.project(expr)
        case obj @ ObjectNode(properties, join) =>
          join match {
            case None =>
            case Some((left, alias, attr @ Attribute(name, column, _, _, ManyToOneType(_, right)))) =>
              obj.idx = sqlBuilder.project(AttributeOQLExpression(List(Ident(name, null)), null, left.table, attr))
              sqlBuilder.leftJoin(left.table, column, right.table, alias, right.pk.get.column)
          }

          properties foreach { case (_, e) => sqlQuery(e) }
        case SequenceNode(seq) =>
        case _                 =>
      }
    }

//    println(prettyPrint(node))

    sqlQuery(node)

    val sql = sqlBuilder.toString

    println(sql)

    execute { c =>
      val rs = c.query(sql)

//      println(TextTable(rs.peer.asInstanceOf[ResultSet]))

      def build(node: Node): Any =
        node match {
          case ArrayNode(entity, element, select, join) =>
            val array = new ArrayBuffer[Any]

            while (rs.next) array += build(element)

            array.toList
          case expr: ExpressionNode => rs get expr.idx
          case obj @ ObjectNode(properties, join) =>
            if (join.isDefined && rs.get(obj.idx) == null) null
            else {
              val map = new mutable.LinkedHashMap[String, Any]

              for ((label, node) <- properties)
                map(label) = build(node)

              map to VectorMap
            }
          case SequenceNode(seq) => ni
        }

      build(node)
    }
  }

}

/**
  * Result node
  */
trait Node

/**
  * One-to-many result node
  *
  * @param entity source entity from which array elements are drawn
  * @param element array element nodes (usually [[ObjectNode]], in future could also be [[ExpressionNode]] resulting
  *                from the "lift" feature [todo] or [[SequenceNode]] resulting from the "tuple" feature)
  * @param select optional boolean condition for selecting elements
  * @param join optional attribute to join on: the attribute contains the target entity to join with
  */
case class ArrayNode(entity: Entity, element: Node, select: Option[OQLExpression], join: Option[Attribute]) extends Node

/**
  * Object (many-to-one) result node
  *
  * @param properties object properties: each property has a name and a node
  */
case class ObjectNode(properties: Seq[(String, Node)], join: Option[(Entity, String, Attribute)]) extends Node { var idx: Int = _ }

/**
  * Sequence result node
  *
  * @param seq node sequence
  */
case class SequenceNode(seq: Seq[Node]) extends Node

/**
  * Result expression
  *
  * @param expr expression (usually [[AttributeOQLExpression]] referring to an entity attribute)
  */
case class ExpressionNode(expr: OQLExpression) extends Node { var idx: Int = _ }
