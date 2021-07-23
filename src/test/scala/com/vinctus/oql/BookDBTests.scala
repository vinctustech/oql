package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.Dynamic.{global => g}

class BookDBTests extends AsyncFreeSpec with Matchers {

  g.require("source-map-support").install()

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db =
    new OQL_NodePG(g.require("fs").readFileSync("test/book.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  def test(oql: String, parameters: (String, Any)*): Future[String] = db.json(oql, parameters: _*)

  "simplest query" in {
    test("book") map { result =>
      result shouldBe
        """
        |[
        |  {
        |    "id": 1,
        |    "title": "Treasure Island",
        |    "year": 1883
        |  },
        |  {
        |    "id": 2,
        |    "title": "Alice's Adventures in Wonderland",
        |    "year": 1865
        |  },
        |  {
        |    "id": 3,
        |    "title": "Oliver Twist",
        |    "year": 1838
        |  },
        |  {
        |    "id": 4,
        |    "title": "A Tale of Two Cities",
        |    "year": 1859
        |  },
        |  {
        |    "id": 5,
        |    "title": "The Adventures of Tom Sawyer",
        |    "year": 1876
        |  },
        |  {
        |    "id": 6,
        |    "title": "Adventures of Huckleberry Finn",
        |    "year": 1884
        |  }
        |]
        |""".trim.stripMargin
    }
  }

  "simplest query with select" in {
    test("book [year > 1880]") map { result =>
      result shouldBe
        """
        |[
        |  {
        |    "id": 1,
        |    "title": "Treasure Island",
        |    "year": 1883
        |  },
        |  {
        |    "id": 6,
        |    "title": "Adventures of Huckleberry Finn",
        |    "year": 1884
        |  }
        |]
        |""".trim.stripMargin
    }
  }

  "simplest query with select using parameter" in {
    test("book [year > :year]", "year" -> 1880) map { result =>
      result shouldBe
        """
        |[
        |  {
        |    "id": 1,
        |    "title": "Treasure Island",
        |    "year": 1883
        |  },
        |  {
        |    "id": 6,
        |    "title": "Adventures of Huckleberry Finn",
        |    "year": 1884
        |  }
        |]
        |""".trim.stripMargin
    }
  }

//  "simplest many-to-one query" in {
//    test("book { * author }") shouldBe
//      """
//        |[
//        |  {
//        |    "id": 1,
//        |    "title": "Treasure Island",
//        |    "year": 1883,
//        |    "author": {
//        |      "id": 1,
//        |      "name": "Robert Louis Stevenson"
//        |    }
//        |  },
//        |  {
//        |    "id": 2,
//        |    "title": "Alice’s Adventures in Wonderland",
//        |    "year": 1865,
//        |    "author": {
//        |      "id": 2,
//        |      "name": "Lewis Carroll"
//        |    }
//        |  },
//        |  {
//        |    "id": 3,
//        |    "title": "Oliver Twist",
//        |    "year": 1838,
//        |    "author": {
//        |      "id": 3,
//        |      "name": "Charles Dickens"
//        |    }
//        |  },
//        |  {
//        |    "id": 4,
//        |    "title": "A Tale of Two Cities",
//        |    "year": 1859,
//        |    "author": {
//        |      "id": 3,
//        |      "name": "Charles Dickens"
//        |    }
//        |  },
//        |  {
//        |    "id": 5,
//        |    "title": "The Adventures of Tom Sawyer",
//        |    "year": 1876,
//        |    "author": {
//        |      "id": 4,
//        |      "name": "Mark Twain"
//        |    }
//        |  },
//        |  {
//        |    "id": 6,
//        |    "title": "Adventures of Huckleberry Finn",
//        |    "year": 1884,
//        |    "author": {
//        |      "id": 4,
//        |      "name": "Mark Twain"
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "many-to-one query with select" in {
//    test("book { title year author { name } } [year > 1880]") shouldBe
//      """
//        |[
//        |  {
//        |    "title": "Treasure Island",
//        |    "year": 1883,
//        |    "author": {
//        |      "name": "Robert Louis Stevenson"
//        |    }
//        |  },
//        |  {
//        |    "title": "Adventures of Huckleberry Finn",
//        |    "year": 1884,
//        |    "author": {
//        |      "name": "Mark Twain"
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "simplest one to many query" in {
//    test("author { name books }") shouldBe
//      """
//        |[
//        |  {
//        |    "name": "Robert Louis Stevenson",
//        |    "books": [
//        |      {
//        |        "id": 1,
//        |        "title": "Treasure Island",
//        |        "year": 1883
//        |      }
//        |    ]
//        |  },
//        |  {
//        |    "name": "Lewis Carroll",
//        |    "books": [
//        |      {
//        |        "id": 2,
//        |        "title": "Alice’s Adventures in Wonderland",
//        |        "year": 1865
//        |      }
//        |    ]
//        |  },
//        |  {
//        |    "name": "Charles Dickens",
//        |    "books": [
//        |      {
//        |        "id": 3,
//        |        "title": "Oliver Twist",
//        |        "year": 1838
//        |      },
//        |      {
//        |        "id": 4,
//        |        "title": "A Tale of Two Cities",
//        |        "year": 1859
//        |      }
//        |    ]
//        |  },
//        |  {
//        |    "name": "Mark Twain",
//        |    "books": [
//        |      {
//        |        "id": 5,
//        |        "title": "The Adventures of Tom Sawyer",
//        |        "year": 1876
//        |      },
//        |      {
//        |        "id": 6,
//        |        "title": "Adventures of Huckleberry Finn",
//        |        "year": 1884
//        |      }
//        |    ]
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "query with select using IN" in {
//    test("book [year IN (1883, 1884)]") shouldBe
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
//  "query with select using IN with parameter" in {
//    test("book [year IN :years]", Map("years" -> List(1883, 1884))) shouldBe
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
//  "query with select using IN with a one-to-many sub-query" in {
//    test("author { name } ['Oliver Twist' IN (books { title })]") shouldBe
//      """
//        |[
//        |  {
//        |    "name": "Charles Dickens"
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "query with select using EXISTS with a one-to-many sub-query" in {
//    test("author { name } [EXISTS (books [year < 1840])]") shouldBe
//      """
//        |[
//        |  {
//        |    "name": "Charles Dickens"
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "query with select using value from a one-to-many sub-query in a comparison" in {
//    test("author { name } [(books { count(*) }) = 2]") shouldBe
//      """
//        |[
//        |  {
//        |    "name": "Charles Dickens"
//        |  },
//        |  {
//        |    "name": "Mark Twain"
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "count query with select using value from a one-to-many sub-query in a comparison" in {
//    db.count("author [(books { count(*) }) > 1]") shouldBe 2
//  }
//
//  "simplest query with ordering" in {
//    test("author <name>") shouldBe
//      """
//        |[
//        |  {
//        |    "id": 3,
//        |    "name": "Charles Dickens"
//        |  },
//        |  {
//        |    "id": 2,
//        |    "name": "Lewis Carroll"
//        |  },
//        |  {
//        |    "id": 4,
//        |    "name": "Mark Twain"
//        |  },
//        |  {
//        |    "id": 1,
//        |    "name": "Robert Louis Stevenson"
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "simplest query with descending ordering" in {
//    test("author <name DESC>") shouldBe
//      """
//        |[
//        |  {
//        |    "id": 1,
//        |    "name": "Robert Louis Stevenson"
//        |  },
//        |  {
//        |    "id": 4,
//        |    "name": "Mark Twain"
//        |  },
//        |  {
//        |    "id": 2,
//        |    "name": "Lewis Carroll"
//        |  },
//        |  {
//        |    "id": 3,
//        |    "name": "Charles Dickens"
//        |  }
//        |]
//        |""".trim.stripMargin
//  }

}
