package com.vinctus.oql

import scala.concurrent.Future

import xyz.hyperreal.rdb_sjs
import xyz.hyperreal.rdb_sjs.{CreateResult, InsertResult, RelationResult}


class RDBConnection(val dataSource: RDBDataSource,data: String) extends OQLConnection {


  private val client =
    new rdb_sjs.Connection {
      if (data ne null)
        load(data, doubleSpaces = true)
    }

  def command(sql: String): Future[RDBResultSet] =

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  def close(): Unit = ???

}
