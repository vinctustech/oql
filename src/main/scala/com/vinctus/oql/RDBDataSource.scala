package com.vinctus.oql

import scala.scalajs.js

class RDBDataSource(data: String) extends SQLDataSource {

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
      case TimestampType         => "TIMESTAMP WITHOUT TIME ZONE"
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case IntegerType => "SERIAL"
      case BigintType  => "BIGSERIAL"
      case _: DataType => mapType(typ)
    }

  def reverseMapType(typ: String): DataType =
    typ match {
      case "timestamp without time zone" => TimestampType
      case "uuid"                        => UUIDType
      case "integer"                     => IntegerType
      case "bigint"                      => BigintType
      case "double precision"            => FloatType
    }

  val resultArrayFunctionStart: String = "to_json(ARRAY("
  val resultArrayFunctionEnd: String = "))"
  val rowSequenceFunctionStart: String = "json_build_array("
  val rowSequenceFunctionEnd: String = ")"
  val typeFunction: Option[String] = Some("pg_typeof(?)")
  val convertFunction: Option[String] = None
  val functionReturnType: Map[(String, Int), List[DataType] => DataType] =
    Map[(String, Int), List[DataType] => DataType](
      ("count", 1) -> (_ => BigintType),
      ("min", 1) -> (_.head),
      ("max", 1) -> (_.head),
      ("avg", 1) -> (_ => FloatType)
    )
  val builtinVariables = Map("CURRENT_DATE" -> DateType, "CURRENT_TIMESTAMP" -> TimestampType, "CURRENT_TIME" -> TimeType)

  override def string(s: String): String = super.string(s).substring(1) // we don't want the 'E' prefix for RDB's version of SQL

}
