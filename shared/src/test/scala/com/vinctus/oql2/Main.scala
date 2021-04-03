package com.vinctus.oql2

object Main extends App {

  val input =
    """
      |entity asdf (asdf1) {
      | x
      |}
      |
      |entity zxcv (zxcv2) {
      | y
      |}
      |""".trim.stripMargin

  println(DMLParse(input))

}
