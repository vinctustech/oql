package com.vinctus.oql2

import scala.concurrent.Future

abstract class OQLConnection {

  val dataSource: OQLDataSource

  def query(query: String): Future[OQLResultSet]

  def insert(command: String): Future[OQLResultSet]

  def execute(command: String): Future[Unit]

  def create(model: DataModel): Future[Unit]

  def close(): Unit

}
