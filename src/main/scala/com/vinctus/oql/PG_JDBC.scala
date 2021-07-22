package com.vinctus.oql

class PG_JDBC(val host: String, val port: Int, val database: String, val user: String, val password: String)(
    implicit ec: scala.concurrent.ExecutionContext)
    extends JDBCDataSource("org.postgresql.Driver")
    with PGDataSource {

  val name = "PostgreSQL (JDBC)"
  val url = s"jdbc:postgresql://$host:$port/$database"

  def connect: OQLConnection = new PGJDBCConnection(this)

}
