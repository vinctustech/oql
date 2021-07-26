package com.vinctus.oql

trait DMLAST
trait DMLDeclaration extends DMLAST
case class DMLEnum(name: Ident, labels: List[Ident]) extends DMLDeclaration
case class DMLModel(decls: Seq[DMLDeclaration]) extends DMLAST
case class DMLEntity(name: Ident, actualName: Option[Ident], attributes: Seq[DMLAttribute]) extends DMLDeclaration
case class DMLAttribute(name: Ident, actualName: Option[Ident], typ: DMLTypeSpecifier, pk: Boolean, required: Boolean) extends DMLAST

trait DMLTypeSpecifier extends DMLAST
trait DMLColumnType extends DMLTypeSpecifier
trait DMLDataType extends DMLColumnType
case class DMLSimpleDataType(typ: String) extends DMLDataType
case class DMLParametricDataType(typ: String, parameters: List[String]) extends DMLDataType

case class DMLEnumType(typ: Ident) extends DMLDataType

trait DMLEntityType extends DMLTypeSpecifier { val entity: Ident }
case class DMLManyToOneType(entity: Ident) extends DMLEntityType with DMLColumnType
case class DMLOneToOneType(entity: Ident, attr: Option[Ident]) extends DMLEntityType
case class DMLOneToManyType(entity: Ident, attr: Option[Ident]) extends DMLEntityType
case class DMLManyToManyType(entity: Ident, link: Ident) extends DMLEntityType
