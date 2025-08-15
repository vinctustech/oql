package com.vinctus.oql

import scala.scalajs.js
import scala.compiletime.uninitialized

class JSResultBuilder extends ResultBuilder {

  private var array: js.Array[Any] = uninitialized
  private var obj: js.Object       = uninitialized

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
