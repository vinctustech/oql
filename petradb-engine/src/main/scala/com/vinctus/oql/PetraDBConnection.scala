package com.vinctus.oql

import io.github.edadma.petradb.{QueryResult, InsertResult, UpdateResult}
import io.github.edadma.petradb.engine.{MemoryDB, PersistentDB, TextDB, Session, executeSQL}

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.matching.Regex

class PetraDBConnection(val dataSource: PetraDBDataSource, storageType: String = "memory", path: String = "")(implicit
    ec: scala.concurrent.ExecutionContext
) extends OQLConnection:

  val db = storageType match {
    case "memory"     => new MemoryDB
    case "persistent" => PersistentDB.create(path, 4096)
    case "text"       => TextDB.open(path)
    case _            => sys.error(s"Unknown storage type: $storageType")
  }

  given session: Session = db.connect()

  def command(sql: String): Future[PetraDBResultSet] =
    Future(
      new PetraDBResultSet(
        executeSQL(sql).head match
          case QueryResult(table)     => table.data.iterator
          case InsertResult(_, table) => table.data.iterator
          case UpdateResult(_)        => Iterator()
      )
    )

  private val varRegex = """\$([0-9_]+)""".r

  def substitute(s: String, parameters: IndexedSeq[Any]): String =
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
      case a: js.Array[?] => s"(${a map subsrender mkString ","})"
      case _              => String.valueOf(a)

  def raw(sql: String, parameters: IndexedSeq[Any]): Future[Seq[Seq[Any]]] =
    val sql1 = substitute(sql, parameters)
    val res =
      executeSQL(sql1).head match
        case QueryResult(table) =>
          table.data map (_.data map unpack)

    Future(res)

  def rawMulti(sql: String): Future[Seq[IndexedSeq[IndexedSeq[Any]]]] =
    val res =
      executeSQL(sql) map {
        case QueryResult(table)     => table.data map (_.data map unpack)
        case InsertResult(_, table) => table.data map (_.data map unpack)
      }

    Future(res)

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] =
    Future(executeSQL(dataSource.schema(model)))

  def close(): Unit = ???
