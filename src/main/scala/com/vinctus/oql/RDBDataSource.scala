package com.vinctus.oql

import scala.scalajs.js

class RDBDataSource(implicit ec: scala.concurrent.ExecutionContext) extends SQLDataSource {

  val name: String = "RDB"

  val connect: RDBConnection = new RDBConnection(this)

  val platformSpecific: PartialFunction[Any, String] = { case d: js.Date =>
    s""""${d.toISOString()}""""
  }

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType              => "TEXT"
      case SmallintType          => "SMALLINT"
      case IntegerType           => "INTEGER"
      case BooleanType           => "BOOLEAN"
      case BigintType            => "BIGINT"
      case DecimalType(p, s)     => s"DECIMAL($p, $s)"
      case DateType              => "DATE"
      case FloatType             => "DOUBLE"
      case UUIDType              => "UUID"
      case TimestampType         => "TIMESTAMP"
      case JSONType              => "JSON"
      case EnumType(name, _)     => name
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case SmallintType => "SMALLINT AUTO"
      case IntegerType  => "INT AUTO"
      case BigintType   => "BIGINT AUTO"
      case UUIDType     => "UUID AUTO"
      case _: Datatype  => mapType(typ)
    }

  def reverseMapType(typ: String): Datatype =
    typ match {
      case "TIMESTAMP" => TimestampType
      case "UUID"      => UUIDType
      case "SMALLINT"  => SmallintType
      case "INT"       => IntegerType
      case "BIGINT"    => BigintType
      case "DOUBLE"    => FloatType
      case "number"    => FloatType
    }

  val resultArrayFunctionStart: String = "TABLE("
  val resultArrayFunctionEnd: String = ")"
  val rowSequenceFunctionStart: String = ""
  val rowSequenceFunctionEnd: String = ""
  val typeFunction: Option[String] = Some("TYPEOF(?)")
  val convertFunction: Option[String] = None
  val caseSensitive: Boolean = true
  val functionReturnType: Map[(String, Int), Seq[Datatype] => Datatype] =
    Map[(String, Int), Seq[Datatype] => Datatype](
      ("count", 1) -> (_ => IntegerType), // todo: this should really be 'BigintType'
      ("min", 1) -> (_.head),
      ("max", 1) -> (_.head),
      ("avg", 1) -> (_ => FloatType)
    )
  val builtinVariables =
    Map("CURRENT_DATE" -> DateType, "CURRENT_TIMESTAMP" -> TimestampType, "CURRENT_TIME" -> TimeType)

  override def string(s: String): String = s"'${quote(s)}'"

}
