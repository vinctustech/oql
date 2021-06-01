package com.vinctus.oql

import scala.concurrent.Future

class RDBConnection(val dataSource: RDBDataSource) extends OQLConnection {

  def command(sql: String): Future[RDBResultSet] = ???

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  def close(): Unit = ???

}
