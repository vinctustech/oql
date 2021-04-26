package com.vinctus.oql2

object Main extends App with BookDB {

  println(db.ds.schema(db.model) mkString "\n\n")
  db.showQuery()
  println(test("book"))

}
