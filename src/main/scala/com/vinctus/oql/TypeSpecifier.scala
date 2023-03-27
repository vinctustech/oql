package com.vinctus.oql

trait TypeSpecifier {
  val isDataType: Boolean; val isColumnType: Boolean; val isArrayType: Boolean;
  def asDatatype: Datatype = asInstanceOf[Datatype]
}
trait Datatype extends TypeSpecifier { val isDataType = true; val isColumnType = true; val isArrayType = false }

case class EnumType(name: String, labels: List[String]) extends Datatype
case object TextType extends Datatype
case object IntegerType extends Datatype
case object BooleanType extends Datatype
case object BigintType extends Datatype
case class DecimalType(precision: Int, scale: Int) extends Datatype
case object DateType extends Datatype
case object TimeType extends Datatype
case object IntervalType extends Datatype
case object FloatType extends Datatype
case object UUIDType extends Datatype
case object TimestampType extends Datatype
case object JSONType extends Datatype

trait RelationalType extends TypeSpecifier {
  val entity: Entity; val isDataType = false; val isColumnType = false; val isArrayType = false
}
case class ManyToOneType(entity: Entity) extends RelationalType { override val isColumnType = true }
case class OneToOneType(entity: Entity, attribute: Attribute) extends RelationalType

trait ArrayRelationalType extends RelationalType { override val isArrayType = true }
case class OneToManyType(entity: Entity, attribute: Attribute) extends ArrayRelationalType
case class ManyToManyType(entity: Entity, link: Entity, self: Attribute, target: Attribute) extends ArrayRelationalType
