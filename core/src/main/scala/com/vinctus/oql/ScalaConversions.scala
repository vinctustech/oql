package com.vinctus.oql

import com.vinctus.sjs_utils.fromJS
import java.time.Instant
import scala.scalajs.js

object ScalaConversions extends Conversions {

  def timestamp(t: String): Any = Instant.parse(if (t.endsWith("Z")) t else t :+ 'Z')

  def uuid(id: String): Any = id

  def bigint(n: String): Any = n.toLong

  def decimal(n: String, precision: Int, scale: Int): Any = BigDecimal(n).setScale(scale)

  def jsonNodePG(v: Any): Any = v match {
    case s: String => JSON.readValue(s)
    case other     => other
  }

  def jsonSequence(v: Any): Any = v

  def array(arr: Any, elementType: Datatype): Any = {
    if (arr == null) null
    else {
      val jsArr = arr.asInstanceOf[js.Array[Any]]
      val converted = elementType match {
        case IntegerType                   => jsArr.map(v => if (v == null) null else v.toString.toInt)
        case FloatType                     => jsArr.map(v => if (v == null) null else v.toString.toDouble)
        case BigintType                    => jsArr.map(v => if (v == null) null else bigint(v.toString))
        case UUIDType                      => jsArr.map(v => if (v == null) null else uuid(v.toString))
        case TimestampType                 => jsArr.map(v => if (v == null) null else timestamp(v.toString))
        case DecimalType(precision, scale) => jsArr.map(v => if (v == null) null else decimal(v.toString, precision, scale))
        case JSONType                      => jsArr.map(v => if (v == null) null else jsonNodePG(v))
        case _                             => jsArr
      }
      converted.toList
    }
  }

}
