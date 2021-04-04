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
case class DMLPrimitiveType(name: String) extends DMLTypeSpecifier
case class DMLManyToOneType(name: Ident) extends DMLTypeSpecifier
//case object DMLText extends DMLPrimitiveType
//case object DMLInteger extends DMLPrimitiveType
//case object DMLBoolean extends DMLPrimitiveType
//case object DMLBigint extends DMLPrimitiveType
//case object DMLDecimal extends DMLPrimitiveType
//case object DMLDate extends DMLPrimitiveType
//case object DMLFloat extends DMLPrimitiveType
//case object DMLUUID extends DMLPrimitiveType
//case object DMLTimestamp extends DMLPrimitiveType
