package com.vinctus.oql

import io.github.edadma.rdb.{Row, TableValue}
import pprint.*

class RDBResultSet(rs: Iterator[Row]) extends OQLResultSet {
  private var row: Row = _

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  def get(idx: Int): OQLResultSetValue = RDBResultSetValue(row.data(idx))

  def getString(idx: Int): String = row.data(idx).string

  def getResultSet(idx: Int): OQLResultSet = new RDBResultSet(row.data(idx).asInstanceOf[TableValue].data.iterator)
}

case class RDBResultSetValue(v: Any) extends OQLResultSetValue
