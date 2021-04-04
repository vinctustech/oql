package com.vinctus.oql2

import xyz.hyperreal.pretty._

object Main extends App {

  val input =
    """
      |entity asdf {
      | x: [poiu] (lkj)
      | y: [zxcv] . qwer (lkj)
      |}
      |""".trim.stripMargin

  println(prettyPrint(DMLParse(input)))

}
