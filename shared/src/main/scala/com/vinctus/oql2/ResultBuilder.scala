package com.vinctus.oql2

abstract class ResultBuilder {

  type Array
  type Object

  def newArray: ResultBuilder

  def +=(elem: Any): ResultBuilder

  def newObject: ResultBuilder

  def update(key: String, value: Any): ResultBuilder

  def arrayResult: Array

  def objectResult: Object

}
