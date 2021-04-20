package com.vinctus.oql2

import java.sql.Statement

class PGConnection(dataSource: JDBCDataSource) extends JDBCConnection(dataSource) {

  override def query(query: String): OQLResultSet = new JDBCResultSet(stmt.executeQuery(query))

  def insert(command: String): JDBCResultSet = {
    stmt.executeUpdate(command, Statement.RETURN_GENERATED_KEYS)
    new JDBCResultSet(stmt.getGeneratedKeys)
  }

}
