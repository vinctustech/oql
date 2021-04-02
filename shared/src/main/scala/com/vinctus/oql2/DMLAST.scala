package com.vinctus.oql2

import org.antlr.v4.runtime.Token

case class Ident(tok: Token, s: String)

trait DMLAST
case class DMLModel(entities: Seq[DMLEntity]) extends DMLAST
case class DMLEntity(name: Ident) extends DMLAST

trait DMLTypeSpecifier
trait DMLPrimitiveType extends DMLTypeSpecifier
case object DMLText extends DMLPrimitiveType
case object DMLInteger extends DMLPrimitiveType
case object DMLBoolean extends DMLPrimitiveType
case object DMLBigint extends DMLPrimitiveType
case object DMLDecimal extends DMLPrimitiveType
case object DMLDate extends DMLPrimitiveType
case object DMLFloat extends DMLPrimitiveType
case object DMLUUID extends DMLPrimitiveType
case object DMLTimestamp extends DMLPrimitiveType

case class DMLAttribute(pk: Any, //Option[Any],
                        attr: Ident,
                        actualField: Any, //Option[Ident],
                        typ: DMLTypeSpecifier,
                        required: Any) //Option[Any])
