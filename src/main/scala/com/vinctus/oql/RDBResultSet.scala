package com.vinctus.oql

import xyz.hyperreal.rdb_sjs.Tuple

class RDBResultSet(rs: Iterator[Tuple]) extends OQLResultSet with JSONResultSet {
  private var row: Tuple = _

  def next: Boolean =
    if (rs.hasNext) {
      row = rs.next()
      true
    } else false

  def get(idx: Int): Any = row(idx)

  def getString(idx: Int): String = row(idx).toString

}
