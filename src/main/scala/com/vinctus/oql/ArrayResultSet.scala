package com.vinctus.oql

trait ArrayResultSet { self: OQLResultSet =>

//  def getResultSet(idx: Int): OQLResultSet = null //new JDBCResultSet(rs.getArray(idx + 1).getResultSet)
  def getResultSet(idx: Int): OQLResultSet = new SequenceResultSet(get(idx).v.asInstanceOf[IndexedSeq[IndexedSeq[Any]]])

}
