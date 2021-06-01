package com.vinctus.oql

abstract class ResultBuilder {

  def newArray: ResultBuilder

  def +=(elem: Any): ResultBuilder

  def newObject: ResultBuilder

  def update(key: String, value: Any): ResultBuilder

  def arrayResult: Any

  def objectResult: Any

}
