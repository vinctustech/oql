package com.vinctus.oql2

trait ArrayResultSet { self: OQLResultSet =>

  def getResultSet(idx: Int): OQLResultSet = null //new JDBCResultSet(rs.getArray(idx + 1).getResultSet)

}
