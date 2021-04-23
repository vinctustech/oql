package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class EventDBTests extends AnyFreeSpec with Matchers with EventDB {

  "uuid, timestamp test with many-to-many inner query" in {
    test("attendee { * events <when> } <name>") shouldBe
      """
        |[
        |  {
        |    "id": e8d982cd-dd19-4766-a627-ab33009bc259,
        |    "name": "me",
        |    "events": [
        |      {
        |        "id": 8aef4c68-7977-48cb-ba38-2764881d0843,
        |        "what": "woke up this morning",
        |        "when": "2021-04-21T06:30:00Z",
        |        "duration": null
        |      },
        |      {
        |        "id": 797f15ab-56ba-4389-aca1-5c3c661fc9fb,
        |        "what": "start testing timestamps",
        |        "when": "2021-04-21T17:42:49.943Z",
        |        "duration": 300.0
        |      }
        |    ]
        |  }
        |]
        |""".trim.stripMargin
  }

}
