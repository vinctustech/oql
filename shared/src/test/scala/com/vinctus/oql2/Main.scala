package com.vinctus.oql2

import xyz.hyperreal.pretty._

object Main extends App {

  val input = "a { b }"

  println(prettyPrint(OQLParse(input)))

}
