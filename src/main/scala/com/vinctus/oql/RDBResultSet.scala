package com.vinctus.oql

import io.github.edadma.rdb.{
  ArrayValue,
  NumberValue,
  ObjectValue,
  Row,
  TableValue,
  TextValue,
  TimestampValue,
  UUIDValue,
  Value
}
import io.github.edadma.dal
import pprint.*

import scala.scalajs.js

class RDBResultSet(rs: Iterator[Row]) extends OQLResultSet {
  private var row: Row = _

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  private def unpack(v: Value): Any =
    v match
      case NumberValue(dal.IntType, value)    => value.intValue
      case NumberValue(dal.DoubleType, value) => value.doubleValue
      case NumberValue(dal.LongType, value)   => value.doubleValue() // todo: js hack
      case TextValue(s)                       => s
      case UUIDValue(id)                      => id
      case TimestampValue(t)                  => new js.Date(t.toISOString) // todo: js hack
      case ArrayValue(data)                   => data map unpack
      case ObjectValue(properties)            => properties map { case (k, v) => k -> unpack(v) } toMap

  def get(idx: Int): OQLResultSetValue = RDBResultSetValue(unpack(row.data(idx)))

  def getString(idx: Int): String = row.data(idx).string

  def getResultSet(idx: Int): OQLResultSet = new RDBResultSet(row.data(idx).asInstanceOf[TableValue].data.iterator)
}

case class RDBResultSetValue(v: Any) extends OQLResultSetValue
