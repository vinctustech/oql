//package com.vinctus.oql2
//
//import org.scalatest.freespec.AnyFreeSpec
//import org.scalatest.matchers.should.Matchers
//
//class BasicTests extends AnyFreeSpec with Matchers with BookDB {
//
//  "simplest query" in {
//    test("book") shouldBe
//      """
//        |[
//        |  {
//        |    "id": 1,
//        |    "title": "Treasure Island",
//        |    "year": 1883
//        |  },
//        |  {
//        |    "id": 2,
//        |    "title": "Alice’s Adventures in Wonderland",
//        |    "year": 1865
//        |  },
//        |  {
//        |    "id": 3,
//        |    "title": "Oliver Twist",
//        |    "year": 1838
//        |  },
//        |  {
//        |    "id": 4,
//        |    "title": "A Tale of Two Cities",
//        |    "year": 1859
//        |  },
//        |  {
//        |    "id": 5,
//        |    "title": "The Adventures of Tom Sawyer",
//        |    "year": 1876
//        |  },
//        |  {
//        |    "id": 6,
//        |    "title": "Adventures of Huckleberry Finn",
//        |    "year": 1884
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "simplest query with select" in {
//    test("book [year > 1880]") shouldBe
//      """
//        |[
//        |  {
//        |    "id": 1,
//        |    "title": "Treasure Island",
//        |    "year": 1883
//        |  },
//        |  {
//        |    "id": 6,
//        |    "title": "Adventures of Huckleberry Finn",
//        |    "year": 1884
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//}
