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
        |        "firstName": "Bruce",
        |        "manager": {
        |          "firstName": "Alexander"
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
        |  }
        |]
        |""".trim.stripMargin
  }

  "count(*)" in {
    test("employee { count(*) }") shouldBe
      """
        |[
        |  {
        |    "count": 5
        |  }
        |]
        |""".trim.stripMargin
  }

  "function with label" in {
    test("employee { employeeCount: count(*) }") shouldBe
      """
        |[
        |  {
        |    "employeeCount": 5
        |  }
        |]
        |""".trim.stripMargin
  }

  "simple many-to-many query" in {
    test("department { departmentName jobs }") shouldBe
      """
        |[
        |  {
        |    "departmentName": "IT",
        |    "jobs": [
        |      {
        |        "id": 20,
        |        "jobTitle": "IT Manager"
        |      },
        |      {
        |        "id": 9,
        |        "jobTitle": "Programmer"
        |      }
        |    ]
        |  },
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
        |  }
        |]
        |""".trim.stripMargin
  }

  "simple many-to-many query with inner query" in {
    test("department { departmentName jobs { jobTitle } }") shouldBe
      """
        |[
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
        |  },
        |  {
        |    "departmentName": "Executive",
        |    "jobs": [
        |      {
        |        "jobTitle": "President"
        |      },
        |      {
        |        "jobTitle": "Administration Vice President"
        |      },
        |      {
        |        "jobTitle": "Administration Vice President"
        |      }
        |    ]
        |  }
        |]
        |""".trim.stripMargin
  }

  "dot notation in project" in {
    test("employee { firstName manager: manager.firstName }") shouldBe
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

  "dot notation in select" in {
    test("employee { firstName } [manager.firstName = 'Steven']") shouldBe
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

  "query with select using IN with a many-to-many sub-query" in {
    test("job { jobTitle } ['IT' IN (departments { departmentName })]") shouldBe
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

  "query with select using EXISTS with a many-to-many sub-query" in {
    test("job { jobTitle } [EXISTS (departments [departmentName = 'IT'])]") shouldBe
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
