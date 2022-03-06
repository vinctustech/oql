package com.vinctus.oql

import io.github.edadma.rdb
import io.github.edadma.rdb.{CreateTableResult, InsertResult, MemoryDB, QueryResult, UpdateResult, executeSQL}

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.matching.Regex

class RDBConnection(val dataSource: RDBDataSource)(implicit ec: scala.concurrent.ExecutionContext)
    extends OQLConnection:

  val db = new MemoryDB

  def command(sql: String): Future[RDBResultSet] =
    Future(
      new RDBResultSet(
        executeSQL(sql)(db).head match
//      case CreateTableResult(_) => Iterator()
          case QueryResult(table)    => table.data.iterator
          case InsertResult(_, auto) => auto.data.iterator
//      case UpdateResult(_)      => Iterator()
      )
    )

  private val varRegex = "$([0-9_]+)".r

  def substitute(s: String, parameters: IndexedSeq[Any]): String = // todo: unit tests for parameters
    if (parameters.isEmpty) s
    else
      varRegex.replaceAllIn(
        s,
        m =>
          val idx = m.group(1).toInt

          if idx >= parameters.length then sys.error(s"substitute: parameter '$idx' not found")
          else Regex.quoteReplacement(subsrender(parameters(idx)))
      )

  def subsrender(a: Any): String =
    a match
      case s: String =>
        s"'${s
            .replace("\\", """\\""")
            .replace("'", """\'""")
            .replace("\r", """\r""")
            .replace("\n", """\n""")}'"
      case d: js.Date     => s"'${d.toISOString()}'"
      case a: js.Array[_] => s"(${a map subsrender mkString ","})"
      case _              => String.valueOf(a)

  def raw(sql: String, parameters: IndexedSeq[Any]): Future[Seq[Seq[Any]]] =
    val sql1 = substitute(sql, parameters)
    val res =
      executeSQL(sql1)(db).head match
        case QueryResult(table) =>
          table.data map (_.data map unpack)

    Future(res)

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
