package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class CarsDBTests extends AsyncFreeSpec with Matchers {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db =
    new OQL_NodePG(g.require("fs").readFileSync("test/cars.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  def test(oql: String, parameters: (String, Any)*): Future[String] = db.json(oql, parameters = parameters.toMap)

  "simple enum query" in {
    test("car [color = 'blue']") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "make": "aston martin",
          |    "color": "blue"
          |  }
          |]
          |""".trim.stripMargin
    }
  }

}
