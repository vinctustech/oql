package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MinimalEmployeeDBTests extends AnyFreeSpec with Matchers with MinimalEmployeeDB {

  "simplest self-join query" in {
    test("employee { firstName manager { firstName } }") shouldBe
      """
        |""".trim.stripMargin
  }

}
