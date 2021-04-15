package com.vinctus.oql2

import com.sun.org.apache.xpath.internal.ExpressionNode
import com.vinctus.oql2.StarOQLProject.label
import sun.jvm.hotspot.HelloWorld.e

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

  private def attributes(entity: Entity, expr: OQLExpression, oql: String): Unit =
    expr match {
      case InfixOQLExpression(left, _, right) =>
        attributes(entity, left, oql)
        attributes(entity, right, oql)
      case attrexp @ AttributeOQLExpression(ids, _, _) =>
        entity.attributes get ids.head.s match {
          case Some(attr) =>
            attrexp.entity = entity
            attrexp.attr = attr
          case None => problem(ids.head.pos, s"entity '${entity.name}' does not have attribute '${ids.head.s}'", oql)
        }
      case _ =>
    }

  private def queryProjects(outer: Option[Entity], query: OQLQuery, oql: String): OQLQuery = {
    val map = new mutable.LinkedHashMap[String, OQLProject]
    val entity =
      if (outer.isDefined) {
        if (query.entity ne null) {
          query.entity
        } else {
          outer.get.attributes get query.resource.s match {
            case Some(attr @ Attribute(name, column, pk, required, ManyToOneType(entity))) =>
              query.entity = entity
              query.attr = attr
              entity
            case None => problem(query.resource.pos, s"entity '${outer.get} does not have attribute '${query.resource.s}'", oql)
          }
        }
      } else
        model.entities get query.resource.s match {
          case Some(e) =>
            query.entity = e
            e
          case None => problem(query.resource.pos, s"unknown entity '${query.resource.s}'", oql)
        }
    val subtracts = new mutable.HashSet[String]

    query.project foreach {
      case p @ QueryOQLProject(label, query) =>
        queryProjects(Some(entity), query, oql)
        query.select foreach (attributes(query.entity, _, oql))
        map(label.s) = p
      case StarOQLProject =>
        entity.attributes.values foreach {
          case attr @ Attribute(name, column, pk, required, typ) if typ.isDataType =>
            map(name) = ExpressionOQLProject(Ident(name), AttributeOQLExpression(List(Ident(name)), entity, attr))
          case _ => // non-datatype attributes don't get included with '*'
        }
      case SubtractOQLProject(id) =>
        if (subtracts(id.s))
          problem(id.pos, s"attribute '${id.s}' already removed", oql)

        subtracts += id.s

        if (map contains id.s)
          map -= id.s
        else
          problem(id.pos, s"attribute '${id.s}' not added with '*'", oql)
      case expProj @ ExpressionOQLProject(label, expr) =>
        if (map contains label.s)
          problem(label.pos, s"duplicate attribute label '${label.s}'", oql)

        map(label.s) = expr match {
          case a @ AttributeOQLExpression(List(id), _, _) =>
            entity.attributes get id.s match {
              case Some(attr @ Attribute(_, _, _, _, _: DataType)) =>
                a.entity = entity
                a.attr = attr
                expProj
              case Some(attr @ Attribute(_, _, _, _, ManyToOneType(mtoEntity))) =>
                QueryOQLProject(
                  label,
                  queryProjects(Some(entity), OQLQuery(id, mtoEntity, attr, List(StarOQLProject), None, None, None, OQLRestrict(None, None)), oql))
              case Some(attr @ Attribute(_, _, _, _, OneToManyType(otmEntity, otmAttr))) =>
                QueryOQLProject(
                  label,
                  queryProjects(Some(entity), OQLQuery(id, otmEntity, attr, List(StarOQLProject), None, None, None, OQLRestrict(None, None)), oql))
              case None => problem(id.pos, s"unknown attribute '${id.s}'", oql)
            }
          case _ =>
            attributes(entity, expr, oql)
            expProj
        }
    }

    query.project = map.values.toList
    query
  }

  private def objectNode(projects: List[OQLProject]): ObjectNode = {
    ObjectNode(projects map { p =>
      (p.label.s, p match {
        case ExpressionOQLProject(label, expr) => ValueNode(expr)
        case QueryOQLProject(label, query) =>
          query.attr.typ match {
            case ManyToOneType(mtoEntity)       => ManyToOneNode(query.entity, query.attr, objectNode(query.project))
            case OneToManyType(otmEntity, attr) => OneToManyNode(query.entity, query.attr, objectNode(query.project))
          }
      })
    })
  }

  def queryMany(oql: String, parameters: Map[String, Any] = Map()) = { //todo: async
    val query =
      OQLParse(oql) match {
        case None              => sys.error("error parsing query")
        case Some(q: OQLQuery) => q
      }

//    println(prettyPrint(query))

    queryProjects(None, query, oql)
    query.select foreach (attributes(query.entity, _, oql))

    val root: ResultNode = ResultNode(query.entity, objectNode(query.project), query.select)

    val sqlBuilder = new SQLQueryBuilder

    def writeQuery(node: Node, table: String): Unit = {
      node match {
        case ResultNode(entity, element, select) =>
          sqlBuilder.table(entity.table)

          if (select.isDefined)
            sqlBuilder.select(select.get, entity.table)

          writeQuery(element, entity.table)
        case e @ ValueNode(expr)    => e.idx = sqlBuilder.project(expr, table)
        case ObjectNode(properties) => properties foreach { case (_, e) => writeQuery(e, table) }
        case n @ ManyToOneNode(entity, attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity)), element) =>
          val alias = s"$table$$$name"

          n.idx = sqlBuilder.project(AttributeOQLExpression(List(Ident(name)), null, attr), table)
          sqlBuilder.leftJoin(table, column, entity.table, alias, entity.pk.get.column)
          writeQuery(element, alias)
        case n @ OneToManyNode(entity, attr @ Attribute(name, column, pk, required, OneToManyType(mtoEntity, otmAttr)), element) =>
          val alias = s"$table$$$name"

      }
    }

//    println(prettyPrint(root))

    writeQuery(root, null)

    val sql = sqlBuilder.toString

    println(sql)

    execute { c =>
      val rs = c.query(sql)

//      println(TextTable(rs.peer.asInstanceOf[ResultSet]))

      def buildResult(node: Node): Any =
        node match {
          case ResultNode(entity, element, select) =>
            val array = new ArrayBuffer[Any]

            while (rs.next) array += buildResult(element)

            array.toList
          case n @ ManyToOneNode(entity, attr, element) =>
            if (rs.get(n.idx) == null) null
            else buildResult(element)
          case v: ValueNode => rs get v.idx
          case ObjectNode(properties) =>
            val map = new mutable.LinkedHashMap[String, Any]

            for ((label, node) <- properties)
              map(label) = buildResult(node)

            map to VectorMap
//          case SequenceNode(seq) => ni
        }

      buildResult(root)
    }
  }

}

trait Node

case class ResultNode(entity: Entity, element: Node, select: Option[OQLExpression]) extends Node

case class ManyToOneNode(entity: Entity, attr: Attribute, element: Node) extends Node { var idx: Int = _ }

case class OneToManyNode(entity: Entity, attribute: Attribute, element: Node) extends Node { var idx: Int = _ }

case class ObjectNode(properties: Seq[(String, Node)]) extends Node // todo: objects as a way of grouping expressions

case class TupleNode(elements: Seq[Node]) extends Node // todo: tuples as a way of grouping expressions

case class ValueNode(value: OQLExpression) extends Node { var idx: Int = _ }
