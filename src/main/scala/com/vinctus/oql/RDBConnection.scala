package com.vinctus.oql

import xyz.hyperreal.rdb_sjs
import xyz.hyperreal.rdb_sjs.{CreateResult, InsertResult, RelationResult, UpdateResult}

import scala.concurrent.Future

class RDBConnection(val dataSource: RDBDataSource, data: String)(implicit ec: scala.concurrent.ExecutionContext) extends OQLConnection {

  private val client =
    new rdb_sjs.Connection {
      if (data ne null)
        load(data, doubleSpaces = true)
    }

  def command(sql: String): Future[RDBResultSet] =
    Future(new RDBResultSet(client.executeSQLStatement(sql) match {
      case CreateResult(_)          => Iterator()
      case RelationResult(relation) => relation.iterator
      case InsertResult(auto, _)    => Iterator(auto.head.values.toIndexedSeq)
      case UpdateResult(_)          => Iterator()
    }))

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  def close(): Unit = ???

}
