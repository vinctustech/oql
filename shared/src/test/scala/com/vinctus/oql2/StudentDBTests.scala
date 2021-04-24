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

  "many-to-many" in {
    test("class { name students { name } <name> } [name LIKE 'S%'] <name>") shouldBe
      """
        |[
        |  {
        |    "name": "Science",
        |    "students": [
        |      {
        |        "name": "Debbie"
        |      },
        |      {
        |        "name": "John"
        |      }
        |    ]
        |  },
        |  {
        |    "name": "Spanish",
        |    "students": [
        |      {
        |        "name": "John"
        |      }
        |    ]
        |  }
        |]
        |""".trim.stripMargin
  }

  "grouped" in {
    test("enrollment { student { name count(*) } } /student.name/ <student.name>") shouldBe
      """
        |[
        |  {
        |    "student": {
        |      "name": "Debbie",
        |      "count": 4
        |    }
        |  },
        |  {
        |    "student": {
        |      "name": "John",
        |      "count": 3
        |    }
        |  }
        |]
        |""".trim.stripMargin
  }

  "recursion" in {
    test("student { * classes { * students <name> } <name> } [name = 'John']") shouldBe
      """
        |[
        |  {
        |    "id": 1,
        |    "name": "John",
        |    "classes": [
        |      {
        |        "id": 9,
        |        "name": "Physical Education",
        |        "students": [
        |          {
        |            "id": 2,
        |            "name": "Debbie"
        |          },
        |          {
        |            "id": 1,
        |            "name": "John"
        |          }
        |        ]
        |      },
        |      {
        |        "id": 5,
        |        "name": "Science",
        |        "students": [
        |          {
        |            "id": 2,
        |            "name": "Debbie"
        |          },
        |          {
        |            "id": 1,
        |            "name": "John"
        |          }
        |        ]
        |      },
        |      {
        |        "id": 3,
        |        "name": "Spanish",
        |        "students": [
        |          {
        |            "id": 1,
        |            "name": "John"
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".trim.stripMargin
  }

}
