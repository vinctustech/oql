package com.vinctus.oql

import io.github.edadma.rdb.{
  ArrayValue,
  NullValue,
  NumberValue,
  ObjectValue,
  Row,
  TableValue,
  TextValue,
  TimestampValue,
  UUIDValue,
  EnumValue,
  Value
}
import io.github.edadma.dal

import scala.scalajs.js
import js.JSConverters._

class RDBResultSet(rs: Iterator[Row]) extends OQLResultSet {
  private var row: Row = _

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  def get(idx: Int): OQLResultSetValue = RDBResultSetValue(unpack(row.data(idx)))

  def getString(idx: Int): String = row.data(idx).string

  def getResultSet(idx: Int): OQLResultSet = new RDBResultSet(row.data(idx).asInstanceOf[TableValue].data.iterator)
}

def unpack(v: Value): Any =
  v match
    case NumberValue(dal.IntType, value)    => value.intValue
    case NumberValue(dal.DoubleType, value) => value.doubleValue
    case NumberValue(dal.LongType, value)   => value.doubleValue // todo: js hack
    case NumberValue(dal.BigDecType, value) =>
      value.toString // todo: js hack (to retain decimals even if they are zero)
    case TextValue(s)      => s
    case UUIDValue(id)     => id
    case TimestampValue(t) => new js.Date(t.toISOString) // todo: js hack
    case ArrayValue(data)  => (data map unpack) toJSArray // todo: js hack
    case ObjectValue(properties) =>
      (properties map { case (k, v) => k -> unpack(v) }).toMap toJSDictionary // todo: js hack
    case NullValue()           => null
    case EnumValue(value, typ) => typ.labels(value)

case class RDBResultSetValue(value: Any) extends OQLResultSetValue
