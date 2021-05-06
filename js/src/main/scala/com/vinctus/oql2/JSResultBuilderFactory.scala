package com.vinctus.oql2

import scala.scalajs.js

object JSResultBuilderFactory extends ResultBuilderFactory {

  def newResultBuilder: ResultBuilder = new JSResultBuilder

  def timestamp(t: String): Any = new js.Date(t)

  def uuid(id: String): Any = id

  def long(n: String): Any = n.toDouble

  def decimal(n: String): Any = n.toDouble

}
