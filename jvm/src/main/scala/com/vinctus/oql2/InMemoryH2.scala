package com.vinctus.oql2

class InMemoryH2 extends JDBCOQLDataSource("org.h2.Driver") {

  val name = "H2 (in memory)"
  val databaseURL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
  val databaseUser = ""
  val databasePassword = ""

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType                 => "VARCHAR(255)"
      case IntegerType              => "INT"
      case BooleanType              => "BOOLEAN"
      case BigintType               => "BIGINT"
      case DecimalType(p, s)        => s"DECIMAL($p, $s)"
      case DateType                 => "DATE"
      case FloatType                => "DOUBLE"
      case UUIDType                 => "UUID"
      case TimestampType            => "TIMESTAMP"
      case ManyToOneType(_, entity) => mapType(entity.pk.get.typ)
    }

  def connect: Connection =
    new Connection {

      val dataSource: OQLDataSource = InMemoryH2.this

    }

}
