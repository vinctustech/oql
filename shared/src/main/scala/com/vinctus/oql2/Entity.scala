package com.vinctus.oql2

case class Entity(name: String, table: String) {

  private[oql2] var _attributes: Map[String, Attribute] = _
  lazy val attributes: Map[String, Attribute] = _attributes
  private[oql2] var _pk: Option[Attribute] = _
  lazy val pk: Option[Attribute] = _pk

}
