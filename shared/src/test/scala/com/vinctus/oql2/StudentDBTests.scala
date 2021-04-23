package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class StudentDBTests extends AnyFreeSpec with Matchers with StudentDB {

  "many-to-one" in {
    test("enrollment { student { name } class { name } grade } <grade> |2, 3|") shouldBe
      """
        |[
        |  {
        |    "student": {
        |      "name": "John"
        |    },
        |    "class": {
        |      "name": "Spanish"
        |    },
        |    "grade": "B+"
        |  },
        |  {
        |    "student": {
        |      "name": "Debbie"
        |    },
        |    "class": {
        |      "name": "Physical Education"
        |    },
        |    "grade": "B+"
        |  }
        |]
        |""".trim.stripMargin
  }

}
