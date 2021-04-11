package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MinimalEmployeeDBTests extends AnyFreeSpec with Matchers with MinimalEmployeeDB {

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

}
