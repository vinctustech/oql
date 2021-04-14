package com.vinctus.oql2

import com.sun.org.apache.xpath.internal.ExpressionNode
import sun.jvm.hotspot.HelloWorld.e

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import xyz.hyperreal.pretty._

import java.sql.ResultSet
import xyz.hyperreal.table.TextTable

import scala.annotation.tailrec
import scala.collection.immutable.VectorMap
import scala.reflect.internal.NoPhase.id

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

    def queryProjects(query: OQLQuery): List[OQLProject] = {
      val map = new mutable.LinkedHashMap[String, OQLProject]
      val entity =
        if (query.entity eq null) {
          model.entities get query.resource.s match {
            case Some(e) => e
            case None    => problem(query.resource.pos, s"unknown entity '${query.resource.s}'", oql)
          }
        } else query.entity

      query.project foreach {
        case p @ QueryOQLProject(label, _) => map(label) = p
        case StarOQLProject =>
          entity.attributes.values.filter(_.typ.isDataType) foreach {
            case attr @ Attribute(name, column, pk, required, typ) =>
              map(name) = ExpressionOQLProject(Ident(name), AttributeOQLExpression(List(Ident(name)), entity, attr))
          }
      }

      map.values.toList
    }

    def objectNode(entity: Entity, table: String, project: List[OQLProject], join: Option[(Entity, String, Attribute)]): ObjectNode = {
      val props = new mutable.LinkedHashMap[String, Node]
      val attrset = new mutable.HashSet[String]
      val subtracts = new mutable.HashSet[String]

      for (p <- project)
        p match {
          case AttributeOQLProject(label, id) =>
            entity.attributes get id.s match {
              case Some(attr @ Attribute(name, column, pk, required, typ)) =>
                val l = label.map(_.s).getOrElse(name)

                if (props contains l)
                  problem(label.getOrElse(id).pos, s"attribute '$l' has already been added", oql)

                props(l) = typ match {
                  case _: DataType => ValueNode(AttributeOQLExpression(List(Ident(name, null)), entity, table, attr))
                  case ManyToOneType(attr_entity) =>
                    val alias = s"$table$$${id.s}"

                    objectNode(attr_entity, alias, List(StarOQLProject), Some((entity, alias, attr)))
                  case OneToManyType(attr_entity, attribute) =>
                    oneToManyNode(OQLQuery(id, attr_entity, List(StarOQLProject), None, None, None, OQLRestrict(None, None)), attribute)
                  case _ => ni
                }
              case None => problem(id.pos, s"unknown attribute '${id.s}'", oql)
            }
          case ExpressionOQLProject(label, expr) =>
            if (props contains label.get.s)
              problem(label.get.pos, s"attribute '${label.get.s}' has already been added", oql)

            props(label.get.s) = ValueNode(expr)
          case QueryOQLProject(label, query) =>
            entity.attributes get query.resource.s match {
              case Some(Attribute(name, column, pk, required, typ))
                  if !typ.isArrayType &&
                    (query.select.isDefined || query.group.isDefined || query.order.isDefined || query.restrict != OQLRestrict(None, None)) =>
                problem(query.resource.pos, s"attribute '${query.resource.s}' is not an array type", oql)
              case Some(attr @ Attribute(name, column, pk, required, typ)) =>
                val l = label.map(_.s).getOrElse(name)

                if (props contains l)
                  problem(label.getOrElse(query.resource).pos, s"attribute '$l' has already been added", oql)

                props(l) = typ match {
                  case ManyToOneType(attr_entity) =>
                    val alias = s"$table$$${query.resource.s}"

                    objectNode(attr_entity, alias, query.project, Some((entity, alias, attr)))
                  case OneToManyType(attr_entity, attribute) =>
                    oneToManyNode(query.copy(entity = attr_entity), attribute)
                }
              // case one to many
              // case many to many
              case None => problem(query.resource.pos, s"unknown attribute '${query.resource.s}'", oql)
            }
          case StarOQLProject =>
            entity.attributes.values.filter(_.typ.isDataType) foreach {
              case attr @ Attribute(name, column, pk, required, typ) =>
                props(name) = ValueNode(AttributeOQLExpression(List(Ident(name, null)), entity, table, attr))
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

    def resultNode(query: OQLQuery): ResultNode = {
      val entity =
        model.entities get query.resource.s match {
          case Some(e) => e
          case None    => problem(query.resource.pos, s"unknown entity '${query.resource.s}'", oql)
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

      query.select foreach (references(_, query.resource.s)) // 'query.resource.s' should really be an alias
      ResultNode(entity, objectNode(entity, query.resource.s, query.project, None), query.select)
    }

    def oneToManyNode(query: OQLQuery, attr: Attribute): OneToManyNode = {
      val entity = query.entity

      println(query)
      println(attr)
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

      query.select foreach (references(_, query.resource.s)) // 'query.resource.s' should really be an alias
      OneToManyNode(entity, query.resource.s, objectNode(entity, query.resource.s, query.project, None), query.select, attr)
    }

    val root: ResultNode = resultNode(query)

    val sqlBuilder = new SQLQueryBuilder

    def writeSQL(node: Node): Unit = {
      node match {
        case OneToManyNode(entity, resource, element, select, Attribute(name, column, pk, required, ManyToOneType(mtoEntity))) =>
          sqlBuilder.leftJoin(mtoEntity.table, mtoEntity.pk.get.column, entity.table, resource, column)

          if (select.isDefined)
            sqlBuilder.select(select.get)

          writeSQL(element)
        case ResultNode(entity, element, select) =>
          sqlBuilder.table(entity.table)

          if (select.isDefined)
            sqlBuilder.select(select.get)

          writeSQL(element)
        case e @ ValueNode(expr) => e.idx = sqlBuilder.project(expr)
        case obj @ ObjectNode(properties, join) =>
          join match {
            case None =>
            case Some((left, alias, attr @ Attribute(name, column, _, _, ManyToOneType(right)))) =>
              obj.idx = sqlBuilder.project(AttributeOQLExpression(List(Ident(name, null)), null, left.table, attr))
              sqlBuilder.leftJoin(left.table, column, right.table, alias, right.pk.get.column)
          }

          properties foreach { case (_, e) => writeSQL(e) }
        case SequenceNode(seq) =>
        case _                 =>
      }
    }

//    println(prettyPrint(node))

    writeSQL(root)

    val sql = sqlBuilder.toString

    println(sql)

    execute { c =>
      val rs = c.query(sql)

//      println(TextTable(rs.peer.asInstanceOf[ResultSet]))

      def build(node: Node): Any =
        node match {
          case ResultNode(entity, element, select) =>
            val array = new ArrayBuffer[Any]

            while (rs.next) array += build(element)

            array.toList
          case expr: ValueNode => rs get expr.idx
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

      build(root)
    }
  }

}

trait Node

case class ResultNode(entity: Entity, element: Node, select: Option[OQLExpression]) extends Node

case class ManyToOneNode(table: String, column: String, join: String, pk: String, element: Node) extends Node

//case class OneToManyNode(entity: Entity, attribute: Attribute, element: Node) extends Node { var idx: Int = _ }

case class ObjectNode(properties: Seq[(String, Node)]) extends Node // todo: objects as a way of grouping expressions

case class TupleNode(elements: Seq[Node]) extends Node // todo: tuples as a way of grouping expressions

case class ValueNode(value: OQLExpression) extends Node { var idx: Int = _ }
