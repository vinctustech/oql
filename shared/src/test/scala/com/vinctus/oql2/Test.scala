package com.vinctus.oql2

import xyz.hyperreal.importer.{Column, Importer, Table}
import xyz.hyperreal.pretty.prettyPrint

import scala.language.postfixOps
import scala.sys.process._

trait Test {

  /*
    DB values:

    "h2": H2 in memory database
    "pg": PostgreSQL in a docker container called "pg-docker" listening on port 5432 (password: "docker")
          terminal command: `psql -h localhost -U postgres -d postgres`
   */
  val DB = "h2"

  val ds: SQLDataSource =
    DB match {
      case "pg" =>
        "docker container stop pg-docker" !

        "docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres" !

        Thread.sleep(2000)

        new PG("localhost", 5432, "postgres", "postgres", "docker")
      case "h2" => new H2mem
    }

  val db: OQL

  def test(oql: String, parameters: Map[String, Any] = Map()): String = db.json(oql, parameters)

  def testmap(oql: String, parameters: Map[String, Any] = Map()): String = prettyPrint(db.queryMany(oql, parameters), classes = true)

  def insert(data: String): Unit = {
    val tables: Iterable[Table] = Importer.importFromString(data, doubleSpaces = true)
    val q = '"'

    for (Table(name, header, data) <- tables) {
      val row =
        data map (r =>
          r map {
            case s: String => s"'${s.replace("'", "''")}'"
            case v         => v
          } mkString ("(", ", ", ")")) mkString ", "

      db.execute(_.insert(s"INSERT INTO $q$name$q (${header map { case Column(name, _, _) => s"$q$name$q" } mkString ", "}) VALUES $row"))
    }
  }

}
