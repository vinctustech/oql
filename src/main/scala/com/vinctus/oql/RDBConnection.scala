package com.vinctus.oql

import io.github.edadma.rdb
import io.github.edadma.rdb.{CreateTableResult, InsertResult, MemoryDB, QueryResult, UpdateResult, executeSQL}

import scala.collection.mutable
import scala.concurrent.Future

class RDBConnection(val dataSource: RDBDataSource)(implicit ec: scala.concurrent.ExecutionContext)
    extends OQLConnection:

  val db = new MemoryDB

  def command(sql: String): Future[RDBResultSet] =
    Future(new RDBResultSet(executeSQL(sql)(db).head match {
//      case CreateTableResult(_) => Iterator()
      case QueryResult(table)    => table.data.iterator
      case InsertResult(_, auto) => auto.data.iterator
//      case UpdateResult(_)      => Iterator()
    }))

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] =
    println(dataSource.schema(model))
    Future(executeSQL(dataSource.schema(model))(db))

  def close(): Unit = ???
