package com.vinctus.oql

import io.github.edadma.rdb.Row

class RDBResultSet(rs: Iterator[Row]) extends OQLResultSet with JSONResultSet {
  private var row: Row = _

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  def get(idx: Int): OQLResultSetValue = RDBResultSetValue(row.data(idx))

  def getString(idx: Int): String = row.data(idx).toString

}

case class RDBResultSetValue(v: Any) extends OQLResultSetValue
