package com.vinctus.oql2

abstract class OQLConnection {

  val dataSource: OQLDataSource

  def query(query: String): OQLResultSet

  def execute(command: String): Unit

  def close(): Unit

}
