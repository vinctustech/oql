package com.vinctus.oql2

object SequenceResultSet {

  def fromJSON(json: String) = new SequenceResultSet(JSON.readArray(json).asInstanceOf[IndexedSeq[IndexedSeq[Any]]])

}

class SequenceResultSet(list: IndexedSeq[IndexedSeq[Any]]) extends OQLResultSet {

  var rest: IndexedSeq[IndexedSeq[Any]] = _
  var first = true
  var row: IndexedSeq[Any] = _

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

  def get(idx: Int): Any = row(idx)

  def getString(idx: Int): String = row(idx).toString

  def getResultSet(idx: Int): OQLResultSet = new SequenceResultSet(row(idx).asInstanceOf[IndexedSeq[IndexedSeq[Any]]])

}
