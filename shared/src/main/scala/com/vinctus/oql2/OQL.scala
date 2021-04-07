package com.vinctus.oql2

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
 * @param element array element nodes (usually [[ObjectNode]], in future could also be [[ExpressionNode]] resulting from the "lift" feature [todo])
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
 * Result expression
 *
 * @param expr expression (usually [[AttributeOQLExpression]] referring to an entity attribute)
 */
case class ExpressionNode(expr: OQLExpression) extends Node
