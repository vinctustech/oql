package com.vinctus.oql2

class H2mem extends JDBCDataSource("org.h2.Driver") {

  val name = "H2 (in memory)"
  val url = s"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
  val user = ""
  val password = ""

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType              => "VARCHAR(255)"
      case IntegerType           => "INT"
      case BooleanType           => "BOOLEAN"
      case BigintType            => "BIGINT"
      case DecimalType(p, s)     => s"DECIMAL($p, $s)"
      case DateType              => "DATE"
      case FloatType             => "DOUBLE"
      case UUIDType              => "UUID"
      case TimestampType         => "TIMESTAMP WITH TIME ZONE" // this keeps H2 from converting timestamps to local time
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case BigintType  => "IDENTITY"
      case _: DataType => mapType(typ)
    }

  def connect: OQLConnection = new H2Connection(this)

  val resultArrayFunctionStart: String = "JSON_ARRAY("
  val resultArrayFunctionEnd: String = ")"
  val rowSequenceFunctionStart: String = "JSON_ARRAY("
  val rowSequenceFunctionEnd: String = " NULL ON NULL)"
  val typeFunction: Option[String] = None
  val convertFunction: Option[String] = Some("CONVERT(?, VARCHAR)")
  val functionReturnType = Map("count" -> BigintType)

  def convert(data: Any, typ: String): Any = data

}
