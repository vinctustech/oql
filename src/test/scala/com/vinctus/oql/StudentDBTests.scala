package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class StudentDBTests extends AsyncFreeSpec with Matchers {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db =
    new OQL_NodePG(g.require("fs").readFileSync("test/student.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  def test(oql: String, parameters: (String, Any)*): Future[String] = db.json(oql, parameters = parameters.toMap)

  "many-to-one" in {
    test("enrollment { student { name } class { name } grade } <student.name, grade> |4, 2|") map { result =>
      result shouldBe
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

  "many-to-many" in {
    test("class { name students { name } <name> } [name LIKE 'S%'] <name>") map { result =>
      result shouldBe
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
  }

  "grouped" in {
    test("enrollment { student { name count(*) } } /student.name/ <student.name>") map { result =>
      result shouldBe
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
  }

  "recursion" in {
    test("student { * classes { * students <name> } <name> } [name = 'John']") map { result =>
      result shouldBe
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

  "exists (many-to-many)" in {
    test("student [EXISTS (classes [name = 'Spanish'])]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 1,
          |    "name": "John"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "not/or" in {
    test("enrollment { class { name } } [NOT (semester = 'fall' OR semester = 'winter')] <class.name>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "class": {
          |      "name": "Physical Education"
          |    }
          |  },
          |  {
          |    "class": {
          |      "name": "Science"
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

}
