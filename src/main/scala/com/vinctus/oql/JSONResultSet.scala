package com.vinctus.oql

trait JSONResultSet { self: OQLResultSet =>

  def getResultSet(idx: Int): OQLResultSet = SequenceResultSet.fromJSON(getString(idx))

}
