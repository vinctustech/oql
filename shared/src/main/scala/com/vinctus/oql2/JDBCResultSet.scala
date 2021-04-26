package com.vinctus.oql2

import java.sql.ResultSet

abstract class JDBCResultSet(val rs: ResultSet) extends OQLResultSet {

  def next: Boolean = rs.next

  def get(idx: Int): Any = rs.getObject(idx + 1)

  def getString(idx: Int): String = rs.getString(idx + 1)

  def getResultSet(idx: Int): OQLResultSet //= new JDBCResultSet(rs.getArray(idx + 1).getResultSet)

}
