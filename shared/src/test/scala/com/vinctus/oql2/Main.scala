package com.vinctus.oql2

import xyz.hyperreal.pretty._
import xyz.hyperreal.table.TextTable

import java.nio.file.{Files, Path, Paths}

object Main extends App with BookDB {

  println(test("author <name DESC>"))

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
