package com.vinctus.oql2

import typings.pg.mod.QueryArrayResult

import scala.scalajs.js

class PGNodeResultSet(rs: QueryArrayResult[js.Array[js.Any]]) extends OQLResultSet with JSONResultSet {

  private var first = false
  private var ridx: Int = _
  private var row: js.Array[js.Any] = _

  def next: Boolean = {
    if (first)
      ridx += 1
    else {
      first = true
      ridx = 0
    }

    if (ridx >= rs.rowCount)
      false
    else {
      row = rs.rows(ridx)
      true
    }
  }

  def get(idx: Int): Any = row(idx)

  def getString(idx: Int): String = row(idx).toString

}
