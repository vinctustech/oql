package com.vinctus.oql

trait PGDataSource extends SQLDataSource {

  val host: String
  val port: Int
  val database: String
  val user: String
  val password: String

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType              => "TEXT"
      case IntegerType           => "INTEGER"
      case BooleanType           => "BOOLEAN"
      case BigintType            => "BIGINT"
      case DecimalType(p, s)     => s"NUMERIC($p, $s)"
      case DateType              => "DATE"
      case FloatType             => "DOUBLE PRECISION"
      case UUIDType              => "UUID"
      case TimestampType         => "TIMESTAMP WITHOUT TIME ZONE"
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case IntegerType => "SERIAL"
      case BigintType  => "BIGSERIAL"
      case _: Datatype => mapType(typ)
    }

  def reverseMapType(typ: String): Datatype =
    typ match {
      case "timestamp without time zone" => TimestampType
      case "uuid"                        => UUIDType
      case "integer"                     => IntegerType
      case "bigint"                      => BigintType
      case "time"                        => TimeType
      case "date"                        => DateType
      case "interval"                    => IntervalType
      case "double precision"            => FloatType
    }

  val resultArrayFunctionStart: String = "to_json(ARRAY("
  val resultArrayFunctionEnd: String = "))"
  val rowSequenceFunctionStart: String = "json_build_array("
  val rowSequenceFunctionEnd: String = ")"
  val typeFunction: Option[String] = Some("pg_typeof(?)")
  val convertFunction: Option[String] = None
  val caseSensitive: Boolean = false
  val functionReturnType: Map[(String, Int), Seq[Datatype] => Datatype] =
    Map[(String, Int), Seq[Datatype] => Datatype](
      ("count", 1) -> (_ => IntegerType), // todo: this should really be 'BigintType'
      ("min", 1) -> (_.head),
      ("max", 1) -> (_.head),
      ("avg", 1) -> (_ => FloatType)
    )
  val builtinVariables: Map[String, Datatype] =
    Map("current_date" -> DateType, "current_timestamp" -> TimestampType, "current_time" -> TimeType)

}
