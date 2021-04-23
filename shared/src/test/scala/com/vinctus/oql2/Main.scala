package com.vinctus.oql2

import xyz.hyperreal.pretty._
import xyz.hyperreal.table.TextTable

import java.nio.file.{Files, Path, Paths}

object Main extends App with StudentDB {

  println(db.ds.schema(db.model) mkString "\n\n")
  db.showQuery()
  println(test("enrollment { student { name } class { name } grade } <grade> |2, 3|"))

}
