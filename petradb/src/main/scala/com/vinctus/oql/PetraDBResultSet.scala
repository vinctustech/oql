package com.vinctus.oql

import io.github.edadma.petradb.*
import io.github.edadma.dal

import scala.scalajs.js
import js.JSConverters._
import scala.compiletime.uninitialized

class PetraDBResultSet(rs: Iterator[Row]) extends OQLResultSet {
  private var row: Row = uninitialized

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  def get(idx: Int): OQLResultSetValue = PetraDBResultSetValue(unpack(row.data(idx)))

  def getString(idx: Int): String = row.data(idx).string

  def getResultSet(idx: Int): OQLResultSet = new PetraDBResultSet(row.data(idx).asInstanceOf[TableValue].data.iterator)
}

def unpack(v: Value): Any =
  v match
    case NumberValue(dal.IntType, value)    => value.intValue
    case NumberValue(dal.DoubleType, value) => value.doubleValue
    case NumberValue(dal.LongType, value)   => value.doubleValue
    case NumberValue(dal.BigDecType, value) =>
      value.toString
    case TextValue(s)            => s
    case BooleanValue(b)         => b
    case UUIDValue(id)           => id
    case TimestampValue(t)       => new js.Date(t.toString)
    case DateValue(d)            => d.toString
    case TimeValue(t)            => t.toString
    case IntervalValue(d)        => d.toString
    case ByteaValue(data)        => data
    case ArrayValue(data)        => (data map unpack) toJSArray
    case ObjectValue(properties) =>
      (properties map { case (k, v) => k -> unpack(v) }).toMap toJSDictionary
    case NullValue()           => null
    case EnumValue(value, typ) => typ.labels(value)

case class PetraDBResultSetValue(value: Any) extends SQLBackendResultSetValue
