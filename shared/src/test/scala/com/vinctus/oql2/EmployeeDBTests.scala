package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class EmployeeDBTests extends AnyFreeSpec with Matchers with EmployeeDB {

  "simplest self-join query" in {
    test("employee { * manager }") shouldBe
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

  "one-to-many query with inner query" in {
    test("job { jobTitle employees { firstName } }") shouldBe
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
        |        "firstName": "Alexander"
        |      },
        |      {
        |        "firstName": "Bruce"
        |      }
        |    ]
        |  }
        |]
        |""".trim.stripMargin
  }

  "one-to-many query with inner many-to-one query" in {
    test("job { jobTitle employees { firstName manager { firstName } } }") shouldBe
      """
        |[
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
        |    "jobTitle": "Administration Vice President",
        |    "employees": [
        |      {
        |        "firstName": "Neena",
        |        "manager": {
        |          "firstName": "Steven"
        |        }
        |      },
        |      {
        |        "firstName": "Lex",
        |        "manager": {
        |          "firstName": "Steven"
        |        }
        |      }
        |    ]
        |  },
        |  {
        |    "jobTitle": "Programmer",
        |    "employees": [
        |      {
        |        "firstName": "Alexander",
        |        "manager": {
        |          "firstName": "Lex"
        |        }
        |      },
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
