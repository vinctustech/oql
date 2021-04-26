package com.vinctus.oql2

trait JSONResultSet { self: OQLResultSet =>

  def getResultSet(idx: Int): OQLResultSet = SequenceResultSet.fromJSON(getString(idx))

}
