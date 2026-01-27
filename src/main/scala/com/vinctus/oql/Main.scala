package com.vinctus.oql

import com.vinctus.oql.facades.pg.types
import com.vinctus.oql.facades.TypeId

import scala.scalajs.js
import js.Dynamic.{global => g}

@main def run(): Unit =
  g.require("source-map-support").install()                       // so we get more informative stack traces
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
