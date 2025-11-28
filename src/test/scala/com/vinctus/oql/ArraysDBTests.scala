package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.scalajs.js.Dynamic.{global => g}

class ArraysDBTests extends AsyncFreeSpec with Matchers with Test {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val dm = "arrays"

  "text array query" in {
    test("arrays_test { id, tags } [id = 1]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 1,
          |    "tags": [
          |      "red",
          |      "green",
          |      "blue"
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "integer array query" in {
    test("arrays_test { id, scores } [id = 1]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 1,
          |    "scores": [
          |      1,
          |      2,
          |      3
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "boolean array query" in {
    test("arrays_test { id, flags } [id = 1]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 1,
          |    "flags": [
          |      true,
          |      false,
          |      true
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "float array query" in {
    test("arrays_test { id, amounts } [id = 1]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 1,
          |    "amounts": [
          |      1.5,
          |      2.5,
          |      3.5
          |    ]
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "null array query" in {
    test("arrays_test { id, tags } [id = 4]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 4,
          |    "tags": null
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "empty array query" in {
    test("arrays_test { id, tags } [id = 3]") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "id": 3,
          |    "tags": []
          |  }
          |]
          |""".trim.stripMargin
    }
  }

}
