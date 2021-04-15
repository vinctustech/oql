package com.vinctus.oql2

import java.sql.ResultSet

class JDBCResultSet(rs: ResultSet) extends OQLResultSet {

  def peer: Any = rs

  def next: Boolean = rs.next

  def get(idx: Int): Any = rs.getObject(idx + 1)

}
