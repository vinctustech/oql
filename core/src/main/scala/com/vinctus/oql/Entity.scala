package com.vinctus.oql

import scala.compiletime.uninitialized

case class Entity(name: String, table: String) {

  private[oql] var _attributes: Map[String, Attribute]                                                = uninitialized
  lazy val attributes: Map[String, Attribute]                                                         = _attributes
  private[oql] var _pk: Option[Attribute]                                                             = uninitialized
  lazy val pk: Option[Attribute]                                                                      = _pk
  private[oql] var _fixing: Map[Entity, List[(AttributeOQLExpression, List[ReferenceOQLExpression])]] = uninitialized
  lazy val fixing: Map[Entity, List[(AttributeOQLExpression, List[ReferenceOQLExpression])]]          = _fixing

}
