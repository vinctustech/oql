package com.vinctus.oql2

abstract class ResultBuilder {

  def newArray: ResultBuilder

  def +=(elem: Any): ResultBuilder

  def newObject: ResultBuilder

  def update(key: String, value: Any): ResultBuilder

  def arrayResult: Any

  def objectResult: Any

}