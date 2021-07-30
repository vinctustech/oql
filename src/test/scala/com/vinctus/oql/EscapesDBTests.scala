package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class EscapesDBTests extends AsyncFreeSpec with Matchers with Test {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val dm = "escapes"
  val `escape sequences` = List(Map("s" -> "\ba\ts\nd\rf\"'\\\f"), Map("s" -> "\u03B8"))

  "escape sequences - scala" in {
    testmap("escapes") map (_ shouldBe `escape sequences`)
  }

  "escape sequences - js" in {
    testmapjs("escapes") map (_ shouldBe `escape sequences`)
  }

}
