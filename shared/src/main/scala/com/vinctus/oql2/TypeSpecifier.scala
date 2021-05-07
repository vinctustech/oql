package com.vinctus.oql2

trait TypeSpecifier { val isDataType: Boolean; val isColumnType: Boolean; val isArrayType: Boolean }
trait DataType extends TypeSpecifier { val isDataType = true; val isColumnType = true; val isArrayType = false }

case object TextType extends DataType
case object IntegerType extends DataType
case object BooleanType extends DataType
case object BigintType extends DataType
case class DecimalType(precision: Int, scale: Int) extends DataType
case object DateType extends DataType
case object TimeType extends DataType
case object FloatType extends DataType
case object UUIDType extends DataType
case object TimestampType extends DataType

trait RelationalType extends TypeSpecifier { val entity: Entity; val isDataType = false; val isColumnType = false; val isArrayType = false }
case class ManyToOneType(entity: Entity) extends RelationalType { override val isColumnType = true }
case class OneToOneType(entity: Entity, attribute: Attribute) extends RelationalType

trait ArrayRelationalType extends RelationalType { override val isArrayType = true }
case class OneToManyType(entity: Entity, attribute: Attribute) extends ArrayRelationalType
case class ManyToManyType(entity: Entity, link: Entity, self: Attribute, target: Attribute) extends ArrayRelationalType
