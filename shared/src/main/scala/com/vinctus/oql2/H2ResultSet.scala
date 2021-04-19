package com.vinctus.oql2

import java.sql.ResultSet

class H2ResultSet(rs: ResultSet) extends JDBCResultSet(rs) {

  override def getResultSet(idx: Int): OQLResultSet = ListResultSet.fromJSON(getString(idx))

}
