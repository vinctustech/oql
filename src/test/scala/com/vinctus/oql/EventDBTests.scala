package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class EventDBTests extends AsyncFreeSpec with Matchers {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db =
    new OQL_NodePG(g.require("fs").readFileSync("test/event.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  def test(oql: String, parameters: (String, Any)*): Future[String] = db.json(oql, parameters = parameters.toMap)

  "uuid, timestamp test with many-to-many inner query" in {
    test("attendee { * events <when> } <name>") map { result =>
      result shouldBe
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
    }
  }

}
