package com.vinctus.oql

import com.vinctus.sjs_utils.DynamicMap

import scala.collection.immutable.VectorMap
import scala.collection.mutable.ListBuffer
import scala.compiletime.uninitialized

class ScalaJSResultBuilder extends ResultBuilder {

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

  def objectResult: DynamicMap = new DynamicMap(obj to VectorMap)

}
