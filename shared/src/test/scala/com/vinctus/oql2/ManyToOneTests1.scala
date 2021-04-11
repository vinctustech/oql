package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ManyToOneTests1 extends AnyFreeSpec with Matchers with BookDB {

  "simplest many to one query" in {
    test("book") shouldBe 123
  }
}
