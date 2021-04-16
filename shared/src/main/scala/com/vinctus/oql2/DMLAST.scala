package com.vinctus.oql2

trait DMLAST
case class DMLModel(entities: Seq[DMLEntity]) extends DMLAST
case class DMLEntity(name: Ident, alias: Option[Ident], attributes: Seq[DMLAttribute]) extends DMLAST
case class DMLAttribute(name: Ident, alias: Option[Ident], typ: DMLTypeSpecifier, pk: Boolean, required: Boolean) extends DMLAST

trait DMLTypeSpecifier extends DMLAST
trait DMLColumnType extends DMLTypeSpecifier
trait DMLPrimitiveType extends DMLColumnType
case class DMLSimplePrimitiveType(typ: String) extends DMLPrimitiveType
case class DMLParametricPrimitiveType(typ: String, parameters: List[String]) extends DMLPrimitiveType

trait DMLEntityType extends DMLTypeSpecifier { val entity: Ident }
case class DMLManyToOneType(entity: Ident) extends DMLEntityType with DMLColumnType
case class DMLOneToOneType(entity: Ident, attr: Option[Ident]) extends DMLEntityType
case class DMLOneToManyType(entity: Ident, attr: Option[Ident]) extends DMLEntityType
case class DMLManyToManyType(entity: Ident, link: Ident) extends DMLEntityType
