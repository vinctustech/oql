package com.vinctus.oql2

import scala.scalajs.js

class JSResultBuilder extends ResultBuilder {

  type Array = js.Array[Any]
  type Object = js.Object

  private var array: js.Array[Any] = _
  private var obj: js.Object = _

  def newArray: ResultBuilder = {
    array = new js.Array[Any]
    this
  }

  def +=(elem: Any): ResultBuilder = {
    array.push(elem)
    this
  }

  def newObject: ResultBuilder = {
    obj = new js.Object
    this
  }

  def update(key: String, value: Any): ResultBuilder = {
    obj.asInstanceOf[js.Dictionary[Any]](key) = value
    this
  }

  def arrayResult: js.Array[Any] = array

  def objectResult: js.Object = obj

}
