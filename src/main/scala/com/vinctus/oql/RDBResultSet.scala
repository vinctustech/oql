package com.vinctus.oql

class RDBResultSet(rs: Array[Array[Any]]) extends OQLResultSet with JSONResultSet {
  private var first = false
  private var ridx: Int = _
  private var row: Array[Any] = _

  def next: Boolean = {
    if (first)
      ridx += 1
    else {
      first = true
      ridx = 0
    }

    if (ridx >= rs.length)
      false
    else {
      row = rs(ridx)
      true
    }
  }

  def get(idx: Int): Any = row(idx)

  def getString(idx: Int): String = row(idx).toString

}
