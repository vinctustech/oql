package com.vinctus.oql

case class Entity(name: String, table: String) {

  private[oql] var _attributes: Map[String, Attribute] = _
  lazy val attributes: Map[String, Attribute] = _attributes
  private[oql] var _pk: Option[Attribute] = _
  lazy val pk: Option[Attribute] = _pk
  private[oql] var _fixing: Map[Entity, List[AttributeOQLExpression]] = _
  lazy val fixing: Map[Entity, List[AttributeOQLExpression]] = _fixing

}
