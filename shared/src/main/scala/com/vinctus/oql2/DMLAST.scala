package com.vinctus.oql2

case class Position(line: Int, col: Int)

case class Ident(s: String, pos: Position) {
  def this(s: String, line: Int, col: Int) = this(s, Position(line, col))
}

trait DMLAST
case class DMLModel(entities: Seq[DMLEntity]) extends DMLAST
case class DMLEntity(name: Ident, alias: Option[Ident], attributes: Seq[DMLAttribute]) extends DMLAST
case class DMLAttribute(name: Ident, alias: Option[Ident], typ: DMLTypeSpecifier, pk: Boolean, required: Boolean)
    extends DMLAST

trait DMLTypeSpecifier extends DMLAST
case class DMLPrimitiveType(typ: String) extends DMLTypeSpecifier
case class DMLManyToOneType(typ: Ident) extends DMLTypeSpecifier
case class DMLOneToManyType(typ: Ident, attr: Option[Ident]) extends DMLTypeSpecifier
case class DMLManyToManyType(typ: Ident, attr: Option[Ident], link: Ident) extends DMLTypeSpecifier
