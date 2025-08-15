package com.vinctus.oql

import scala.compiletime.uninitialized

object SequenceResultSet {

  def fromJSON(json: String) = new SequenceResultSet(JSON.readArray(json).asInstanceOf[IndexedSeq[IndexedSeq[Any]]])

}

class SequenceResultSet(list: IndexedSeq[IndexedSeq[Any]]) extends OQLResultSet {

  var rest: IndexedSeq[IndexedSeq[Any]] = uninitialized
  var first                             = true
  var row: IndexedSeq[Any]              = uninitialized

  def next: Boolean = {
    if (first) {
      first = false
      rest = list
    } else
      rest = rest.tail

    if (rest.isEmpty)
      false
    else {
      row = rest.head
      true
    }
  }

  def get(idx: Int): SequenceResultSetValue = SequenceResultSetValue(row(idx))

  def getString(idx: Int): String = row(idx).toString

  def getResultSet(idx: Int): OQLResultSet = new SequenceResultSet(row(idx).asInstanceOf[IndexedSeq[IndexedSeq[Any]]])

}

case class SequenceResultSetValue(value: Any) extends OQLResultSetValue
