package com.vinctus.oql2

object Main extends App {

  val input =
    """
      |entity asdf (asdf1) {
      | *x: text
      |}
      |
      |entity zxcv (zxcv2) {
      | y: qwer!
      |}
      |""".trim.stripMargin

  println(DMLParse(input))

}
