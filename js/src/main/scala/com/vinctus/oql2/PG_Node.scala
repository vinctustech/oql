package com.vinctus.oql2

import scalajs.js

class PG_Node(val domain: String, val port: Int, val database: String, val user: String, val password: String) extends PGDataSource {

  val name: String = "PostgreSQL (node-pg)"

  def timestamp(t: String): Any = new js.Date(t)

  def uuid(id: String): Any = id

  def connect: OQLConnection = ???

}
