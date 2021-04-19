package com.vinctus.oql2

import xyz.hyperreal.json.DefaultJSONReader

import scala.collection.immutable.ArraySeq

object ListResultSet {

  def fromJSON(json: String) = new ListResultSet(DefaultJSONReader.fromString(json).asInstanceOf[List[List[Any]]])

}

class ListResultSet(list: List[Seq[Any]]) extends OQLResultSet {

  var rest: List[Seq[Any]] = _
  var first = true
  var row: ArraySeq[Any] = _

  def next: Boolean = {
    if (first) {
      first = false
      rest = list
    } else
      rest = rest.tail

    if (rest.isEmpty)
      false
    else {
      row = rest.head to ArraySeq
      true
    }
  }

  def get(idx: Int): Any = row(idx)

  def getString(idx: Int): String = row(idx).toString

  def getResultSet(idx: Int): OQLResultSet = ListResultSet.fromJSON(getString(idx))

}
