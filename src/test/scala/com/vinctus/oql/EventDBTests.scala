package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class EventDBTests extends AsyncFreeSpec with Matchers with Test {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val dm = "event"
  val `uuid, timestamp test with many-to-many inner query` =
    """
      |[
      |  {
      |    "id": "e8d982cd-dd19-4766-a627-ab33009bc259",
      |    "name": "me",
      |    "events": [
      |      {
      |        "id": "8aef4c68-7977-48cb-ba38-2764881d0843",
      |        "what": "woke up this morning",
      |        "when": "2021-04-21T06:30:00.000Z",
      |        "duration": null
      |      },
      |      {
      |        "id": "797f15ab-56ba-4389-aca1-5c3c661fc9fb",
      |        "what": "start testing timestamps",
      |        "when": "2021-04-21T17:42:49.943Z",
      |        "duration": 300
      |      }
      |    ]
      |  }
      |]
      |""".trim.stripMargin

  "uuid, timestamp test with many-to-many inner query - scala" in {
    test("attendee { * events <when> } <name>") map (_ shouldBe `uuid, timestamp test with many-to-many inner query`)
  }

  "uuid, timestamp test with many-to-many inner query - js" in {
    testjs("attendee { * events <when> } <name>") map (_ shouldBe `uuid, timestamp test with many-to-many inner query`)
  }

  "overlaps" in {
    test(
      "event {what} [(when, when + concat('3 hours')::INTERVAL) OVERLAPS ('2021-04-21T18:42:49.943Z', '2021-04-21T19:42:49.943Z')]"
    ) map (_ shouldBe
      """
        |[
        |  {
        |    "what": "start testing timestamps"
        |  }
        |]
        |""".trim.stripMargin)
  }

}
