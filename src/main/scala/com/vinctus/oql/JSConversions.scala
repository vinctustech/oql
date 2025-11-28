package com.vinctus.oql

import scala.scalajs.js
import com.vinctus.sjs_utils.toJS

object JSConversions extends Conversions {

  def timestamp(t: String): Any = new js.Date(s"$t+00:00")

  def uuid(id: String): Any = id

  def bigint(n: String): Any = js.BigInt(n)

  def decimal(n: String, precision: Int, scale: Int): Any = n.toDouble

  def jsonNodePG(v: String): Any = js.JSON.parse(v)

  def jsonSequence(v: Any): Any = toJS(v)

  def array(arr: Any, elementType: Datatype): Any = {
    if (arr == null) null
    else {
      val jsArr = arr.asInstanceOf[js.Array[Any]]
      elementType match {
        case IntegerType                   => jsArr.map(v => if (v == null) null else v.toString.toInt)
        case FloatType                     => jsArr.map(v => if (v == null) null else v.toString.toDouble)
        case BigintType                    => jsArr.map(v => if (v == null) null else bigint(v.toString))
        case UUIDType                      => jsArr.map(v => if (v == null) null else uuid(v.toString))
        case TimestampType                 => jsArr.map(v => if (v == null) null else timestamp(v.toString))
        case DecimalType(precision, scale) => jsArr.map(v => if (v == null) null else decimal(v.toString, precision, scale))
        case JSONType                      => jsArr.map(v => if (v == null) null else jsonNodePG(v.asInstanceOf[String]))
        case _                             => jsArr // text, boolean, date, time, interval pass through
      }
    }
  }
}
