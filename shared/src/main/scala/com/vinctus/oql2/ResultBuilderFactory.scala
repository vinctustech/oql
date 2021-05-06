package com.vinctus.oql2

abstract class ResultBuilderFactory {

  def newResultBuilder: ResultBuilder

  def timestamp(t: String): Any

  def uuid(id: String): Any

  def long(n: String): Any

  def decimal(n: String): Any

}
