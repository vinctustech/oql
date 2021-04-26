//package com.vinctus.oql2
//
//import org.scalatest.freespec.AnyFreeSpec
//import org.scalatest.matchers.should.Matchers
//
//class OrdersDBTests extends AnyFreeSpec with Matchers with OrdersDB {
//
//  "aggregate" in {
//    test("agent { * -phone_no orders { sum(ord_amount) } } [working_area = 'Bangalore'] <agent_code>") shouldBe
//      """
//        |[
//        |  {
//        |    "agent_code": "A001",
//        |    "agent_name": "Subbarao",
//        |    "working_area": "Bangalore",
//        |    "commission": 0.14,
//        |    "orders": [
//        |      {
//        |        "sum_ord_amount": 800.00
//        |      }
//        |    ]
//        |  },
//        |  {
//        |    "agent_code": "A007",
//        |    "agent_name": "Ramasundar",
//        |    "working_area": "Bangalore",
//        |    "commission": 0.15,
//        |    "orders": [
//        |      {
//        |        "sum_ord_amount": 2500.00
//        |      }
//        |    ]
//        |  },
//        |  {
//        |    "agent_code": "A011",
//        |    "agent_name": "Ravi Kumar",
//        |    "working_area": "Bangalore",
//        |    "commission": 0.15,
//        |    "orders": [
//        |      {
//        |        "sum_ord_amount": 5000.00
//        |      }
//        |    ]
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "between" in {
//    test("order { sum(ord_amount) count(ord_amount) agent { agent_name } } [ord_amount BETWEEN 3000 AND 4000] /agent.agent_name/ <agent.agent_name>") shouldBe
//      """
//        |[
//        |  {
//        |    "sum_ord_amount": 6500.00,
//        |    "count_ord_amount": 2,
//        |    "agent": {
//        |      "agent_name": "Alford"
//        |    }
//        |  },
//        |  {
//        |    "sum_ord_amount": 4000.00,
//        |    "count_ord_amount": 1,
//        |    "agent": {
//        |      "agent_name": "Ivan"
//        |    }
//        |  },
//        |  {
//        |    "sum_ord_amount": 7500.00,
//        |    "count_ord_amount": 2,
//        |    "agent": {
//        |      "agent_name": "Mukesh"
//        |    }
//        |  },
//        |  {
//        |    "sum_ord_amount": 10500.00,
//        |    "count_ord_amount": 3,
//        |    "agent": {
//        |      "agent_name": "Santakumar"
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//}
