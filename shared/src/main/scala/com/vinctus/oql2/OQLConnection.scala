package com.vinctus.oql2

abstract class OQLConnection {

  val dataSource: OQLDataSource

  def query(query: String): OQLResultSet //todo: async

  def insert(command: String): OQLResultSet //todo: async

  def execute(command: String): Unit //todo: async

  def close(): Unit

}
