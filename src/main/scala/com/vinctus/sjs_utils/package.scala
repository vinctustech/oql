package com.vinctus

import java.time.{Instant, LocalDate}
import scala.collection.immutable.VectorMap
import scala.scalajs.js
import js.JSConverters._

package object sjs_utils {

  def jsArray(v: Any): Boolean = v.isInstanceOf[js.Array[?]]

  def jsObject(v: Any): Boolean =
    js.typeOf(v) == "object" && (v != null) && !v.isInstanceOf[Long] && !v.isInstanceOf[js.Date] && !jsArray(v)

  def toMap(a: js.UndefOr[js.Any], undefinedToNull: Boolean = true): VectorMap[String, Any] =
    fromJS(a, undefinedToNull).asInstanceOf[VectorMap[String, Any]]

  def fromJS(a: js.UndefOr[js.Any], undefinedToNull: Boolean = true): Any =
    if (undefinedToNull && !a.isDefined) null
    else if (jsObject(a))
      a.asInstanceOf[js.Dictionary[js.Any]].iterator map { case (k, v) =>
        (k, fromJS(v, undefinedToNull).asInstanceOf[js.UndefOr[js.Any]])
      } to VectorMap
    else if (jsArray(a))
      a.asInstanceOf[js.Array[js.Any]].iterator map (fromJS(_, undefinedToNull).asInstanceOf[js.UndefOr[js.Any]]) toList
    else a

  def toJS(a: Any): js.Any = {
    a match {
      case Some(a)          => a.asInstanceOf[js.Any]
      case None             => js.undefined
      case date: LocalDate  => new js.Date(date.getYear, date.getMonthValue - 1, date.getDayOfMonth)
      case instant: Instant => new js.Date(instant.toEpochMilli.toDouble)
      case d: BigDecimal    => d.toDouble
      case s: String        => s
      case l: Seq[_]        => l map toJS toJSArray
      case m: Map[_, _]     =>
        (m map { case (k, v) => k -> toJS(v) })
          .asInstanceOf[Map[String, Any]]
          .toJSDictionary
      case _ => a.asInstanceOf[js.Any]
    }
  }
}
