package com.vinctus.oql2

import java.sql.Statement

class InMemoryH2(db: String) extends JDBCDataSource("org.h2.Driver") {

  val name = "H2 (in memory)"
  val url = s"jdbc:h2:mem:$db;DB_CLOSE_DELAY=-1"
  val user = ""
  val password = ""

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

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case BigintType => "IDENTITY"
      case _: DataType          => mapType(typ)
    }

  def connect: OQLConnection =
    new JDBCConnection(this) {
      def insert(command: String): JDBCResultSet = {
        stmt.executeUpdate(command, Statement.RETURN_GENERATED_KEYS)
        new JDBCResultSet(stmt.getGeneratedKeys)
      }
    }

}
