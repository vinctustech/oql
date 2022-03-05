package com.vinctus.oql

import io.github.edadma.rdb
import io.github.edadma.rdb.{CreateTableResult, InsertResult, MemoryDB, QueryResult, UpdateResult, executeSQL}

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js

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

  def raw(sql: String, values: IndexedSeq[Any]): Future[Seq[Any]] = Future(Seq())

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] =
    Future(executeSQL(dataSource.schema(model))(db))

  def close(): Unit = ???

/*
  static async canUserView(customerId: string, userId: string): Promise<boolean> {
    const result = await oql.raw(
      `
      SELECT id FROM customers
      WHERE id = $1
      AND store_id IN (SELECT store_id FROM users_stores us WHERE us.user_id = $2)
    `,
      [customerId, userId]
    )

    if (result && result.length === 1) {
      return true
    }

    return false
  }
 */
