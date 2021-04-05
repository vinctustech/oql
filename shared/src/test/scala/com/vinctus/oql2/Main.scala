package com.vinctus.oql2

import xyz.hyperreal.pretty._

object Main extends App {

  val input = "asdf {a.b lab1: f(c)} [x = 5]"

  println(prettyPrint(OQLParse(input)))

}
