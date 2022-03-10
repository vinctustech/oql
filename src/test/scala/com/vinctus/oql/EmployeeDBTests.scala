package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.Future
import scala.scalajs.js.Dynamic.{global => g}

class EmployeeDBTests extends AsyncFreeSpec with Matchers with Test {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val dm = "employee"

  "simplest self-join query" in {
    test("employee { * manager }") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 100,
          |    "firstName": "Steven",
          |    "lastName": "King",
          |    "manager": null
          |  },
          |  {
          |    "id": 101,
          |    "firstName": "Neena",
          |    "lastName": "Kochhar",
          |    "manager": {
          |      "id": 100,
          |      "firstName": "Steven",
          |      "lastName": "King"
          |    }
          |  },
          |  {
          |    "id": 102,
          |    "firstName": "Lex",
          |    "lastName": "De Haan",
          |    "manager": {
          |      "id": 100,
          |      "firstName": "Steven",
          |      "lastName": "King"
          |    }
          |  },
          |  {
          |    "id": 103,
          |    "firstName": "Alexander",
          |    "lastName": "Hunold",
          |    "manager": {
          |      "id": 102,
          |      "firstName": "Lex",
          |      "lastName": "De Haan"
          |    }
          |  },
          |  {
          |    "id": 104,
          |    "firstName": "Bruce",
          |    "lastName": "Ernst",
          |    "manager": {
          |      "id": 103,
          |      "firstName": "Alexander",
          |      "lastName": "Hunold"
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "one-to-many query with inner query" in {
    test("job { jobTitle employees { firstName } }") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "jobTitle": "President",
          |    "employees": [
          |      {
          |        "firstName": "Steven"
          |      }
          |    ]
          |  },
          |  {
          |    "jobTitle": "Administration Vice President",
          |    "employees": [
          |      {
          |        "firstName": "Neena"
          |      },
          |      {
          |        "firstName": "Lex"
          |      }
          |    ]
          |  },
          |  {
          |    "jobTitle": "Programmer",
          |    "employees": [
          |      {
          |        "firstName": "Bruce"
          |      }
          |    ]
          |  },
          |  {
          |    "jobTitle": "IT Manager",
          |    "employees": [
          |      {
          |        "firstName": "Alexander"
          |      }
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "one-to-many query with inner many-to-one query" in {
    test("job { jobTitle employees { firstName manager { firstName } } <firstName> } <jobTitle>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "jobTitle": "Administration Vice President",
          |    "employees": [
          |      {
          |        "firstName": "Lex",
          |        "manager": {
          |          "firstName": "Steven"
          |        }
          |      },
          |      {
          |        "firstName": "Neena",
          |        "manager": {
          |          "firstName": "Steven"
          |        }
          |      }
          |    ]
          |  },
          |  {
          |    "jobTitle": "IT Manager",
          |    "employees": [
          |      {
          |        "firstName": "Alexander",
          |        "manager": {
          |          "firstName": "Lex"
          |        }
          |      }
          |    ]
          |  },
          |  {
          |    "jobTitle": "President",
          |    "employees": [
          |      {
          |        "firstName": "Steven",
          |        "manager": null
          |      }
          |    ]
          |  },
          |  {
          |    "jobTitle": "Programmer",
          |    "employees": [
          |      {
          |        "firstName": "Bruce",
          |        "manager": {
          |          "firstName": "Alexander"
          |        }
          |      }
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "count(*)" in {
    test("employee { count(*) }") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "count": 5
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "function with label" in {
    test("employee { employeeCount: count(*) }") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "employeeCount": 5
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "simple many-to-many query" in {
    test("department { departmentName jobs <id> } <departmentName>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "departmentName": "Executive",
          |    "jobs": [
          |      {
          |        "id": 4,
          |        "jobTitle": "President"
          |      },
          |      {
          |        "id": 5,
          |        "jobTitle": "Administration Vice President"
          |      },
          |      {
          |        "id": 5,
          |        "jobTitle": "Administration Vice President"
          |      }
          |    ]
          |  },
          |  {
          |    "departmentName": "IT",
          |    "jobs": [
          |      {
          |        "id": 9,
          |        "jobTitle": "Programmer"
          |      },
          |      {
          |        "id": 20,
          |        "jobTitle": "IT Manager"
          |      }
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "simple many-to-many query with inner query" in {
    test("department { departmentName jobs { jobTitle } <jobTitle> } <departmentName>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "departmentName": "Executive",
          |    "jobs": [
          |      {
          |        "jobTitle": "Administration Vice President"
          |      },
          |      {
          |        "jobTitle": "Administration Vice President"
          |      },
          |      {
          |        "jobTitle": "President"
          |      }
          |    ]
          |  },
          |  {
          |    "departmentName": "IT",
          |    "jobs": [
          |      {
          |        "jobTitle": "IT Manager"
          |      },
          |      {
          |        "jobTitle": "Programmer"
          |      }
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "dot notation in project" in {
    test("employee { firstName manager: manager.firstName }") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "firstName": "Steven",
          |    "manager": null
          |  },
          |  {
          |    "firstName": "Neena",
          |    "manager": "Steven"
          |  },
          |  {
          |    "firstName": "Lex",
          |    "manager": "Steven"
          |  },
          |  {
          |    "firstName": "Alexander",
          |    "manager": "Lex"
          |  },
          |  {
          |    "firstName": "Bruce",
          |    "manager": "Alexander"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "dot notation in select" in {
    test("employee { firstName } [manager.firstName = 'Steven']") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "firstName": "Neena"
          |  },
          |  {
          |    "firstName": "Lex"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "query with select using IN with a many-to-many sub-query" in {
    test("job { jobTitle } ['IT' IN (departments { departmentName })]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "jobTitle": "Programmer"
          |  },
          |  {
          |    "jobTitle": "IT Manager"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "query with select using EXISTS with a many-to-many sub-query" in {
    test("job { jobTitle } [EXISTS (departments [departmentName = 'IT'])]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "jobTitle": "Programmer"
          |  },
          |  {
          |    "jobTitle": "IT Manager"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

}
