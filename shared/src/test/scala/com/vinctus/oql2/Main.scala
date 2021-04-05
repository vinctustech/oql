package com.vinctus.oql2

import xyz.hyperreal.pretty._

object Main extends App {

  val input = "a { * b [x = 5] c }"

  println(prettyPrint(OQLParse(input)))

}
