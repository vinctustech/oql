package com.vinctus.oql

import io.github.edadma.petradb.{QueryResult, InsertResult, UpdateResult, DeleteResult}
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
          case _                      => Iterator()
      )
    )

  private val varRegex = """\$([0-9_]+)""".r

  def substitute(s: String, parameters: IndexedSeq[Any]): String =
    if (parameters.isEmpty) s
    else
      varRegex.replaceAllIn(
        s,
        m =>
          val idx = m.group(1).toInt - 1 // PostgreSQL $1-based indexing to 0-based

          if idx < 0 || idx >= parameters.length then sys.error(s"substitute: parameter '${m.group(1)}' not found")
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

  def raw(sql: String, parameters: IndexedSeq[Any]): Future[(IndexedSeq[String], Seq[Seq[Any]])] =
    val res =
      executeSQL(sql, parameters).head match
        case QueryResult(table) =>
          val columns = table.meta.columns.map(_.name)
          val rows = table.data map (_.data map unpack)
          (columns, rows)
        case InsertResult(_, table) =>
          val columns = table.meta.columns.map(_.name)
          val rows = table.data map (_.data map unpack)
          (columns, rows)
        case UpdateResult(_) =>
          (IndexedSeq.empty, Seq.empty)
        case DeleteResult(_) =>
          (IndexedSeq.empty, Seq.empty)
        case _ =>
          (IndexedSeq.empty, Seq.empty)

    Future(res)

  def rawMulti(sql: String): Future[Seq[IndexedSeq[IndexedSeq[Any]]]] =
    val res =
      executeSQL(sql) collect {
        case QueryResult(table)     => table.data map (_.data map unpack)
        case InsertResult(_, table) => table.data map (_.data map unpack)
      }

    Future(res)

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] =
    Future(executeSQL(dataSource.schema(model)))

  def close(): Unit = ???
