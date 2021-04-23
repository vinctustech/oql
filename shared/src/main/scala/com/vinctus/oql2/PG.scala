package com.vinctus.oql2

import java.time.Instant
import java.util.UUID

class PG(domain: String, port: Int, database: String, val user: String, val password: String) extends JDBCDataSource("org.postgresql.Driver") {

  val name = "PostgreSQL"
  val url = s"jdbc:postgresql://$domain:$port/$database"

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

  def connect: OQLConnection = new PGConnection(this)

  val resultArrayFunctionStart: String = "to_json(ARRAY("
  val resultArrayFunctionEnd: String = "))"
  val rowSequenceFunctionStart: String = "json_build_array("
  val rowSequenceFunctionEnd: String = ")"
  val typeFunction: Option[String] = Some("pg_typeof(?)")
  val convertFunction: Option[String] = None
  val functionReturnType = Map("count" -> BigintType)

  def convert(data: Any, typ: String): Any =
    (data, typ) match {
      case (t: String, "timestamp without time zone") => Instant.parse(if (t endsWith "Z") t else s"${t}Z")
      case (t: String, "uuid")                        => UUID.fromString(t)
      case (n: String, "integer")                     => n.toInt
      case (n: String, "bigint")                      => n.toLong
      case (n: String, "double precision")            => n.toDouble
      case _                                          => data
    }

}
