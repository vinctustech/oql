package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global as g

class RacesDBTests extends AsyncFreeSpec with Matchers with Test {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val dm = "races"

  "time" in {
    test("races {runner_name start_day start_time total_miles end_time} [race_name = '5K']") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "runner_name": "bolt",
          |    "start_day": "2022-10-19T04:00:00.000Z",
          |    "start_time": "11:00:00",
          |    "total_miles": 3.1,
          |    "end_time": "2022-10-19T11:22:31.000Z"
          |  },
          |  {
          |    "runner_name": "felix",
          |    "start_day": "2022-10-19T04:00:00.000Z",
          |    "start_time": "11:00:00",
          |    "total_miles": 3.1,
          |    "end_time": "2022-10-19T11:30:50.000Z"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "time difference" in {
    test("races {runner_name start_day total_time: ((end_time - start_time)::time)} [race_name = '5K']") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "runner_name": "bolt",
          |    "start_day": "2022-10-19T04:00:00.000Z",
          |    "total_time": "00:22:31"
          |  },
          |  {
          |    "runner_name": "felix",
          |    "start_day": "2022-10-19T04:00:00.000Z",
          |    "total_time": "00:30:50"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "best time" in {
    test("races {runner_name min_time: min((end_time - start_time)::time)} /runner_name/ <runner_name>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "runner_name": "bolt",
          |    "min_time": "00:06:30"
          |  },
          |  {
          |    "runner_name": "felix",
          |    "min_time": "00:07:15"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "total time" in {
    test("races {runner_name total_time: sum((end_time - start_time)::time)} /runner_name/ <runner_name>") map {
      result =>
        result shouldBe
          """
          |[
          |  {
          |    "runner_name": "bolt",
          |    "total_time": {
          |      "hours": 6,
          |      "minutes": 9,
          |      "seconds": 20
          |    }
          |  },
          |  {
          |    "runner_name": "felix",
          |    "total_time": {
          |      "hours": 8,
          |      "minutes": 2,
          |      "seconds": 29
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

}
