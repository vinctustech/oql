package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class BookDBTests extends AnyFreeSpec with Matchers with BookDB {

  "simplest query" in {
    test("book") shouldBe
      """
        |[
        |  {
        |    "id": 1,
        |    "title": "Treasure Island",
        |    "year": 1883
        |  },
        |  {
        |    "id": 2,
        |    "title": "Alice’s Adventures in Wonderland",
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

  "simplest query with select" in {
    test("book [year > 1880]") shouldBe
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

  "simplest many to one query" in {
    test("book { * author }") shouldBe
      """
        |[
        |  {
        |    "id": 1,
        |    "title": "Treasure Island",
        |    "year": 1883,
        |    "author": {
        |      "id": 1,
        |      "name": "Robert Louis Stevenson"
        |    }
        |  },
        |  {
        |    "id": 2,
        |    "title": "Alice’s Adventures in Wonderland",
        |    "year": 1865,
        |    "author": {
        |      "id": 2,
        |      "name": "Lewis Carroll"
        |    }
        |  },
        |  {
        |    "id": 3,
        |    "title": "Oliver Twist",
        |    "year": 1838,
        |    "author": {
        |      "id": 3,
        |      "name": "Charles Dickens"
        |    }
        |  },
        |  {
        |    "id": 4,
        |    "title": "A Tale of Two Cities",
        |    "year": 1859,
        |    "author": {
        |      "id": 3,
        |      "name": "Charles Dickens"
        |    }
        |  },
        |  {
        |    "id": 5,
        |    "title": "The Adventures of Tom Sawyer",
        |    "year": 1876,
        |    "author": {
        |      "id": 4,
        |      "name": "Mark Twain"
        |    }
        |  },
        |  {
        |    "id": 6,
        |    "title": "Adventures of Huckleberry Finn",
        |    "year": 1884,
        |    "author": {
        |      "id": 4,
        |      "name": "Mark Twain"
        |    }
        |  }
        |]
        |""".trim.stripMargin
  }

  "many to one query with select" in {
    test("book { title year author { name } } [year > 1880]") shouldBe
      """
        |[
        |  {
        |    "title": "Treasure Island",
        |    "year": 1883,
        |    "author": {
        |      "name": "Robert Louis Stevenson"
        |    }
        |  },
        |  {
        |    "title": "Adventures of Huckleberry Finn",
        |    "year": 1884,
        |    "author": {
        |      "name": "Mark Twain"
        |    }
        |  }
        |]
        |""".trim.stripMargin
  }

  "simplest one to many query" in {
    test("author { name books }") shouldBe
      """
        |[
        |  {
        |    "name": "Robert Louis Stevenson",
        |    "books": [
        |      {
        |        "id": 1,
        |        "title": "Treasure Island",
        |        "year": 1883
        |      }
        |    ]
        |  },
        |  {
        |    "name": "Lewis Carroll",
        |    "books": [
        |      {
        |        "id": 2,
        |        "title": "Alice’s Adventures in Wonderland",
        |        "year": 1865
        |      }
        |    ]
        |  },
        |  {
        |    "name": "Charles Dickens",
        |    "books": [
        |      {
        |        "id": 3,
        |        "title": "Oliver Twist",
        |        "year": 1838
        |      },
        |      {
        |        "id": 4,
        |        "title": "A Tale of Two Cities",
        |        "year": 1859
        |      }
        |    ]
        |  },
        |  {
        |    "name": "Mark Twain",
        |    "books": [
        |      {
        |        "id": 5,
        |        "title": "The Adventures of Tom Sawyer",
        |        "year": 1876
        |      },
        |      {
        |        "id": 6,
        |        "title": "Adventures of Huckleberry Finn",
        |        "year": 1884
        |      }
        |    ]
        |  }
        |]
        |""".trim.stripMargin
  }

}
