package com.vinctus.oql2

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

        Thread.sleep(1000)

        new PG("localhost", 5432, "postgres", "postgres", "docker")
      case "h2" => new H2mem
    }

  val db: OQL

  def test(oql: String, parameters: Map[String, Any] = Map()): String = JSON(db.queryMany(oql, parameters), format = true)

}
