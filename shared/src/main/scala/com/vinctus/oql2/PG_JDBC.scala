package com.vinctus.oql2

import java.time.Instant
import java.util.UUID

class PG_JDBC(val domain: String, val port: Int, val database: String, val user: String, val password: String)
    extends JDBCDataSource("org.postgresql.Driver")
    with PGDataSource {

  val name = "PostgreSQL (JDBC)"
  val url = s"jdbc:postgresql://$domain:$port/$database"

  def connect: OQLConnection = new PGJDBCConnection(this)

  def timestamp(t: String): Any = Instant.parse(if (t endsWith "Z") t else s"${t}Z")

  def uuid(id: String): Any = UUID.fromString(id)
}
