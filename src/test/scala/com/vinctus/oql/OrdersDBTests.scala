package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class OrdersDBTests extends AsyncFreeSpec with Matchers with Test {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val dm = "orders"

  // todo: currency amounts don't always get rendered as expected
  "aggregate" in {
    test("agent { * -phone_no orders { sum(ord_amount) } } [working_area = 'Bangalore'] <agent_code>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "agent_code": "A001",
          |    "agent_name": "Subbarao",
          |    "working_area": "Bangalore",
          |    "commission": 0.14,
          |    "orders": [
          |      {
          |        "sum_ord_amount": 800.00
          |      }
          |    ]
          |  },
          |  {
          |    "agent_code": "A007",
          |    "agent_name": "Ramasundar",
          |    "working_area": "Bangalore",
          |    "commission": 0.15,
          |    "orders": [
          |      {
          |        "sum_ord_amount": 2500.00
          |      }
          |    ]
          |  },
          |  {
          |    "agent_code": "A011",
          |    "agent_name": "Ravi Kumar",
          |    "working_area": "Bangalore",
          |    "commission": 0.15,
          |    "orders": [
          |      {
          |        "sum_ord_amount": 5000.00
          |      }
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "between" in {
    test("order { sum(ord_amount) count(ord_amount) agent { agent_name } } [ord_amount BETWEEN 3000 AND 4000] /agent.agent_name/ <agent.agent_name>") map {
      result =>
        result shouldBe
          """
            |[
            |  {
            |    "sum_ord_amount": 6500.00,
            |    "count_ord_amount": 2,
            |    "agent": {
            |      "agent_name": "Alford"
            |    }
            |  },
            |  {
            |    "sum_ord_amount": 4000.00,
            |    "count_ord_amount": 1,
            |    "agent": {
            |      "agent_name": "Ivan"
            |    }
            |  },
            |  {
            |    "sum_ord_amount": 7500.00,
            |    "count_ord_amount": 2,
            |    "agent": {
            |      "agent_name": "Mukesh"
            |    }
            |  },
            |  {
            |    "sum_ord_amount": 10500.00,
            |    "count_ord_amount": 3,
            |    "agent": {
            |      "agent_name": "Santakumar"
            |    }
            |  }
            |]
            |""".trim.stripMargin
    }
  }

}
