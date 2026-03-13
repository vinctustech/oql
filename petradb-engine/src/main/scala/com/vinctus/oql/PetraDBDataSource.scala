package com.vinctus.oql

import scala.scalajs.js

class PetraDBDataSource(storageType: String = "memory", path: String = "")(implicit ec: scala.concurrent.ExecutionContext) extends SQLDataSource {

  val name: String = "PetraDB"

  val connect: PetraDBConnection = new PetraDBConnection(this, storageType, path)

  val platformSpecific: PartialFunction[Any, String] = { case d: js.Date =>
    s""""${d.toISOString()}""""
  }

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType              => "TEXT"
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
      case ArrayType(elemType)   => mapType(elemType) + " ARRAY"
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case IntegerType => "INT AUTO"
      case BigintType  => "BIGINT AUTO"
      case UUIDType    => "UUID AUTO"
      case _: Datatype => mapType(typ)
    }

  def reverseMapType(typ: String): Datatype =
    typ match {
      case "TIMESTAMP" => TimestampType
      case "UUID"      => UUIDType
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
  val caseSensitive: Boolean = false
  val functionReturnType: Map[(String, Int), List[Datatype] => Datatype] =
    Map[(String, Int), List[Datatype] => Datatype](
      // Aggregate functions
      ("count", 1) -> (_ => IntegerType),
      ("sum", 1) -> (_.head),
      ("min", 1) -> (_.head),
      ("max", 1) -> (_.head),
      ("avg", 1) -> (_ => FloatType),
      ("string_agg", 2) -> (_ => TextType),
      ("bool_and", 1) -> (_ => BooleanType),
      ("bool_or", 1) -> (_ => BooleanType),
      // String functions
      ("lower", 1) -> (_ => TextType),
      ("upper", 1) -> (_ => TextType),
      ("length", 1) -> (_ => IntegerType),
      ("trim", 1) -> (_ => TextType),
      ("concat", 2) -> (_ => TextType),
      ("concat", 3) -> (_ => TextType),
      ("concat", 4) -> (_ => TextType),
      ("replace", 3) -> (_ => TextType),
      ("substring", 2) -> (_ => TextType),
      ("substring", 3) -> (_ => TextType),
      ("left", 2) -> (_ => TextType),
      ("right", 2) -> (_ => TextType),
      // Null handling
      ("coalesce", 2) -> (_.head),
      ("coalesce", 3) -> (_.head),
      ("coalesce", 4) -> (_.head),
      ("nullif", 2) -> (_.head),
      // Date/time functions
      ("now", 0) -> (_ => TimestampType),
      ("date_trunc", 2) -> (_ => TimestampType),
      ("date_part", 2) -> (_ => FloatType),
      ("make_timestamp", 6) -> (_ => TimestampType),
      ("make_timestamptz", 6) -> (_ => TimestampType),
      ("make_timestamptz", 7) -> (_ => TimestampType),
      ("age", 1) -> (_ => IntervalType),
      ("age", 2) -> (_ => IntervalType),
      // Math functions
      ("abs", 1) -> (_.head),
      ("ceil", 1) -> (_.head),
      ("ceiling", 1) -> (_.head),
      ("floor", 1) -> (_.head),
      ("round", 1) -> (_.head),
      ("round", 2) -> (_.head),
      ("random", 0) -> (_ => FloatType),
      // Type conversion
      ("to_char", 2) -> (_ => TextType),
      // JSON functions
      ("jsonb_array_length", 1) -> (_ => IntegerType),
    )
  val builtinVariables =
    Map("current_date" -> DateType, "current_timestamp" -> TimestampType, "current_time" -> TimeType)


}
