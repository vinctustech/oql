package com.vinctus.oql2

trait PGDataSource extends SQLDataSource {

  val host: String
  val port: Int
  val database: String
  val user: String
  val password: String

  def timestamp(t: String): Any

  def uuid(id: String): Any

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

  val resultArrayFunctionStart: String = "to_json(ARRAY("
  val resultArrayFunctionEnd: String = "))"
  val rowSequenceFunctionStart: String = "json_build_array("
  val rowSequenceFunctionEnd: String = ")"
  val typeFunction: Option[String] = Some("pg_typeof(?)")
  val convertFunction: Option[String] = None
  val functionReturnType = Map("count" -> BigintType)

  def convert(data: Any, typ: String): Any =
    (data, typ) match {
      case (t: String, "timestamp without time zone") => timestamp(t)
      case (id: String, "uuid")                       => uuid(id)
      case (n: String, "integer")                     => n.toInt
      case (n: String, "bigint")                      => n.toLong
      case (n: String, "double precision")            => n.toDouble
      case _                                          => data
    }

}
