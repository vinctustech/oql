package com.vinctus.oql2

object Main extends App with StudentDB {

  println(db.ds.schema(db.model) mkString "\n\n")
  db.showQuery()
  println(test("enrollment { student { name count(*) } } /student.name/ <student.name>"))

}
