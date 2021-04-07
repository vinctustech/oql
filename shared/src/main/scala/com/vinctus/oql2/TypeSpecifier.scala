package com.vinctus.oql2

trait TypeSpecifier { val isDataType: Boolean }
trait DataType extends TypeSpecifier { val isDataType = true }

case object TextType extends DataType
case object IntegerType extends DataType
case object BooleanType extends DataType
case object BigintType extends DataType
case class DecimalType(precision: Int, scale: Int) extends DataType
case object DateType extends DataType
case object FloatType extends DataType
case object UUIDType extends DataType
case object TimestampType extends DataType

trait RelationalType extends TypeSpecifier { val isDataType = false }
case class ManyToOneType(entityName: String, entity: Entity) extends RelationalType
