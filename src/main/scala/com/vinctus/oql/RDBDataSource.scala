package com.vinctus.oql

import scala.scalajs.js

class RDBDataSource(data: String)(implicit ec: scala.concurrent.ExecutionContext) extends SQLDataSource {

  val name: String = "RDB"

  val connect = new RDBConnection(this, data)

  val platformSpecific: PartialFunction[Any, String] = {
    case d: js.Date => s""""${d.toISOString()}""""
  }

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType              => "TEXT"
      case IntegerType           => "INTEGER"
      case BooleanType           => "BOOLEAN"
      case BigintType            => "BIGINT"
      case DecimalType(p, s)     => s"DECIMAL($p, $s)"
      case DateType              => "DATE"
      case FloatType             => "FLOAT"
      case UUIDType              => "UUID"
      case TimestampType         => "TIMESTAMP"
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case IntegerType => "INT AUTO"
      case BigintType  => "BIGINT AUTO"
      case _: Datatype => mapType(typ)
    }

  def reverseMapType(typ: String): Datatype =
    typ match {
      case "TIMESTAMP" => TimestampType
      case "UUID"      => UUIDType
      case "INT"       => IntegerType
      case "BIGINT"    => BigintType
      case "DOUBLE"    => FloatType
    }

  val resultArrayFunctionStart: String = "ARRAY("
  val resultArrayFunctionEnd: String = ")"
  val rowSequenceFunctionStart: String = ""
  val rowSequenceFunctionEnd: String = ""
  val typeFunction: Option[String] = Some("type(?)")
  val convertFunction: Option[String] = None
  val caseSensitive: Boolean = true
  val functionReturnType: Map[(String, Int), List[Datatype] => Datatype] =
    Map[(String, Int), List[Datatype] => Datatype](
      ("count", 1) -> (_ => IntegerType), // todo: this should really be 'BigintType'
      ("min", 1) -> (_.head),
      ("max", 1) -> (_.head),
      ("avg", 1) -> (_ => FloatType)
    )
  val builtinVariables = Map("current_date" -> DateType, "current_timestamp" -> TimestampType, "current_time" -> TimeType)

  override def string(s: String): String = super.string(s).substring(1) // we don't want the 'E' prefix for RDB's version of SQL

}
