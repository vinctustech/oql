package com.vinctus.oql2

import xyz.hyperreal.pretty._

object Main extends App {

  val input = "entity a (b) { x: int } entity b { y: int }"
  val dml = DMLParse(input)

  println(prettyPrint(dml))

  val model = new DataModel(dml.get, input)

}
