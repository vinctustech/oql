package com.vinctus.oql2

object Main extends App {

  val input =
    """
      |entity asdf {
      | x
      |}
      |
      |entity zxcv {
      | y
      |}
      |""".trim.stripMargin

  println(DMLParse(input))

}
