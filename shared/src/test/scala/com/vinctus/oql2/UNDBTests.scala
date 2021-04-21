package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class UNDBTests extends AnyFreeSpec with Matchers with UNDBPG {

  "simple one-to-one query" in {
    test("country { * rep { name } }") shouldBe
      """
        |[
        |  {
        |    "id": 1,
        |    "name": "Nigeria",
        |    "rep": {
        |      "name": "Abubakar Ahmad"
        |    }
        |  },
        |  {
        |    "id": 2,
        |    "name": "Ghana",
        |    "rep": {
        |      "name": "Joseph Nkrumah"
        |    }
        |  },
        |  {
        |    "id": 3,
        |    "name": "South Africa",
        |    "rep": {
        |      "name": "Lauren Zuma"
        |    }
        |  },
        |  {
        |    "id": 4,
        |    "name": "Republic of China (Taiwan)",
        |    "rep": null
        |  }
        |]
        |""".trim.stripMargin
  }

}
