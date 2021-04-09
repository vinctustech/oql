package com.vinctus.oql2

import xyz.hyperreal.pretty._
import xyz.hyperreal.table.TextTable

import java.sql.ResultSet

object Main extends App {

  val dm =
    """
      |entity a {
      | *id: bigint
      |  x: text
      |  y: int
      |}
      |""".stripMargin
  val oql = new OQL(dm, new InMemoryH2("test"))

  oql.create()
  println(oql.dataSource.asInstanceOf[SQLDataSource].schema(oql.model))
  oql.perform(_.insert("insert into a (x, y) values ('zxcv', 3), ('asdf', 4)"))

// a: x <- 'asdf', y <- y + 1 [y > 5]
  val q = oql.queryMany("a {y x}") // [x = "as'df"]

  println(JSON(q.get, format = true))

//  oql.perform(c => println(TextTable(c.query(q).peer.asInstanceOf[ResultSet])))

}

/*
val input = "entity a { *id: bigint  x: int }"
val dml = DMLParse(input)

//  println(prettyPrint(dml))

val model = new DataModel(dml.get, input)
val h2 = new InMemoryH2("test")

println(h2.schema(model) mkString "\n\n")
h2.create(model)

val conn = h2.connect

conn.insert("insert into a (x) values (3), (4)")

val res = conn.query("select * from a")

println(TextTable(res.peer.asInstanceOf[ResultSet]))
conn.close()
 */
