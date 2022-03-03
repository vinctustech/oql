package com.vinctus.oql

import io.github.edadma.rdb
import io.github.edadma.rdb.{MemoryDB, executeSQL, CreateTableResult, InsertResult, QueryResult, UpdateResult}

import scala.concurrent.Future

class RDBConnection(val dataSource: RDBDataSource)(implicit ec: scala.concurrent.ExecutionContext) extends OQLConnection {

  private val db = new MemoryDB

  def command(sql: String): Future[RDBResultSet] =
    Future(new RDBResultSet(executeSQL(sql)(db).head match {
//      case CreateTableResult(_) => Iterator()
      case QueryResult(table) => table.data.iterator
//      case InsertResult(auto)   => Iterator(auto.values.toIndexedSeq)
//      case UpdateResult(_)      => Iterator()
    }))

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  def close(): Unit = ???

}
