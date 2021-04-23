package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class StudentDBTests extends AnyFreeSpec with Matchers with StudentDB {

  "many-to-one" in {
    test("enrollment { student { name } class { name } grade } <student.name, grade> |4, 2|") shouldBe
      """
        |[
        |  {
        |    "student": {
        |      "name": "Debbie"
        |    },
        |    "class": {
        |      "name": "Physical Education"
        |    },
        |    "grade": "B+"
        |  },
        |  {
        |    "student": {
        |      "name": "Debbie"
        |    },
        |    "class": {
        |      "name": "Biology"
        |    },
        |    "grade": "B-"
        |  },
        |  {
        |    "student": {
        |      "name": "John"
        |    },
        |    "class": {
        |      "name": "Science"
        |    },
        |    "grade": "A"
        |  },
        |  {
        |    "student": {
        |      "name": "John"
        |    },
        |    "class": {
        |      "name": "Spanish"
        |    },
        |    "grade": "B+"
        |  }
        |]
        |""".trim.stripMargin
  }

}
