package com.vinctus.oql

import scala.scalajs.js

object JSConversions extends Conversions {

  def timestamp(t: String): Any = new js.Date(t)

  def uuid(id: String): Any = id

  def long(n: String): Any = n.toDouble

  def decimal(n: String, precision: Int, scale: Int): Any = n.toDouble

}
