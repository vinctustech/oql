package com.vinctus.oql2

import xyz.hyperreal.pretty._
import xyz.hyperreal.table.TextTable

import java.nio.file.{Files, Path, Paths}

object Main extends App with BookDB {

//  val dm = Files.readString(Paths.get("test/books"))
////  val oql = new OQL(dm, new H2_mem("test"))
////
////  oql.create()
//  println(oql.dataSource.asInstanceOf[SQLDataSource].schema(oql.model) mkString "\n\n")
//  oql.execute(_.insert("""insert into author (name) values
//      |('Robert Louis Stevenson'),
//      |('Lewis Carroll'),
//      |('Charles Dickens'),
//      |('Mark Twain')
//      |""".stripMargin))
//  oql.execute(_.insert("""insert into book (title, year, author) values
//                         |('Treasure Island', 1883, 1),
//                         |('Alice’s Adventures in Wonderland', 1865, 2),
//                         |('Oliver Twist', 1838, 3),
//                         |('A Tale of Two Cities', 1859, 3),
//                         |('The Adventures of Tom Sawyer', 1876, 4),
//                         |('Adventures of Huckleberry Finn', 1884, 4)
//                         |""".stripMargin))
//
//// a: x <- 'asdf', y <- y + 1 [y > 5]
//
//  val q = oql.queryMany("book") //"book { title year author { name } } [year > 1880]"

  println(test("book"))

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
