package com.vinctus.oql2

class PG_JDBC(val host: String, val port: Int, val database: String, val user: String, val password: String)
    extends JDBCDataSource("org.postgresql.Driver")
    with PGDataSource {

  val name = "PostgreSQL (JDBC)"
  val url = s"jdbc:postgresql://$host:$port/$database"

  def connect: OQLConnection = new PGJDBCConnection(this)

  def resultBuilder: ResultBuilder = new ScalaResultBuilder

}
