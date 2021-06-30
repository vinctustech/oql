package com.vinctus.oql

import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.scalajs.js
import js.Dynamic.{global => g}

object Main extends App {

  g.require("source-map-support").install() // so we get more informative stack traces
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON

}
