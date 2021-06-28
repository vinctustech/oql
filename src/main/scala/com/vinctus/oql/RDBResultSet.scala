package com.vinctus.oql

class RDBResultSet(rs: Iterator[Array[Any]]) extends OQLResultSet with JSONResultSet {
  private var row: Array[Any] = _

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  def get(idx: Int): Any = row(idx)

  def getString(idx: Int): String = row(idx).toString

}
