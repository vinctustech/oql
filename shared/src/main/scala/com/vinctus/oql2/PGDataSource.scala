package com.vinctus.oql2

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
  val functionReturnType = Map("count" -> BigintType)
  val builtinVariables = Map("CURRENT_DATE" -> DateType, "CURRENT_TIMESTAMP" -> TimestampType, "CURRENT_TIME" -> TimeType)

  private val specialRegex = """(['\\\r\n])""".r

  def quote(s: String): String =
    specialRegex.replaceAllIn(s, _.group(1) match {
      case "'"  => "''"
      case "\\" => """\\"""
      case "\r" => """\r"""
      case "\n" => """\n"""
    })

}
