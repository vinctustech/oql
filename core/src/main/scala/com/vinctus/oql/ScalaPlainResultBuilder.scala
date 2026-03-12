package com.vinctus.oql

import scala.collection.immutable.VectorMap
import scala.collection.mutable.ListBuffer
import scala.compiletime.uninitialized

class ScalaPlainResultBuilder extends ResultBuilder {

  type Array  = List[Any]
  type Object = VectorMap[String, Any]

  private var array: ListBuffer[Any]         = uninitialized
  private var obj: ListBuffer[(String, Any)] = uninitialized

  def newArray: ResultBuilder = {
    array = new ListBuffer
    this
  }

  def +=(elem: Any): ResultBuilder = {
    array += elem
    this
  }

  def newObject: ResultBuilder = {
    obj = new ListBuffer
    this
  }

  def update(key: String, value: Any): ResultBuilder = {
    obj += (key -> value)
    this
  }

  def arrayResult: List[Any] = array.toList

  def objectResult: VectorMap[String, Any] = obj to VectorMap

}
