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
      case ArrayType(elemType)   => mapType(elemType) + "[]"
      case JSONType              => "JSONB"
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
      case "timestamp without time zone" | "timestamp with time zone" => TimestampType
      case "uuid"                           => UUIDType
      case "integer" | "smallint"           => IntegerType
      case "bigint"                         => BigintType
      case "time" | "time without time zone" | "time with time zone" => TimeType
      case "date"                           => DateType
      case "interval"                       => IntervalType
      case "double precision" | "numeric" | "real" => FloatType
      case "text" | "character varying" | "character" => TextType
      case "boolean"                        => BooleanType
      case "json" | "jsonb"                 => JSONType
      case s if s.endsWith("[]")            => ArrayType(reverseMapType(s.dropRight(2)))
      case s if s.startsWith("_")           => ArrayType(reverseMapType(s.drop(1)))
    }

  val resultArrayFunctionStart: String = "to_json(ARRAY("
  val resultArrayFunctionEnd: String = "))"
  val rowSequenceFunctionStart: String = "json_build_array("
  val rowSequenceFunctionEnd: String = ")"
  val typeFunction: Option[String] = Some("pg_typeof(?)")
  val convertFunction: Option[String] = None
  val caseSensitive: Boolean = false
  val functionReturnType: Map[(String, Int), List[Datatype] => Datatype] =
    Map[(String, Int), List[Datatype] => Datatype](
      // Aggregate functions
      ("count", 1) -> (_ => IntegerType), // todo: this should really be 'BigintType'
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
  val builtinVariables: Map[String, Datatype] =
    Map("current_date" -> DateType, "current_timestamp" -> TimestampType, "current_time" -> TimeType)

}
