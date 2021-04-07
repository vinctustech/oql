package com.vinctus.oql2

import scala.collection.mutable.ArrayBuffer

class OQL(dm: String, db: OQLDataSource) {

  val model: DataModel =
    DMLParse(dm) match {
      case None              => sys.error("error building data model")
      case Some(m: DMLModel) => new DataModel(m, dm)
    }

  def entity(name: String): Entity = model.entities(name)

  def queryMany(oql: String, parameters: Map[String, Any]): collection.Seq[Any] = {//todo: async
    val query =
      OQLParse(oql) match {
        case None => sys.error("error parsing query")
        case Some(q: OQLQuery) => q
      }

    def arrayNode(query: OQLQuery, join: Option[Attribute]): ArrayNode = {
      val (entity, attr) =
        join match {
          case Some(_) => ni
          case None =>
            model.entities get query.entity.s match {
              case Some(e) => (e, None)
              case None => problem(query.entity.pos, s"unknown entity '${query.entity.s}'", oql)
            }
        }

      val attrs = new ArrayBuffer[(String, Node)]

      for (p <- query.project)
        p match {
          case ExpressionOQLProject(label, expr) =>
          case QueryOQLProject(label, query) =>
          case StarOQLProject =>
            entity.attributes.values.filter(_.typ.isDataType) foreach {
              case Attribute(name, column, pk, required, typ) =>
            }
          case SubtractOQLProject(id) =>
          case _ =>
        }

      ArrayNode(entity, ObjectNode(attrs.toList), query.select, join)
    }

    arrayNode(query, None)

    Nil
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
case class ObjectNode(properties: Seq[(String, Node)]) extends Node

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
case class ExpressionNode(expr: OQLExpression) extends Node
