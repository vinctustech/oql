package com.vinctus.oql2

import java.sql.Statement

class H2Connection(dataSource: JDBCDataSource) extends JDBCConnection(dataSource) {

  def insert(command: String): JDBCResultSet = {
    stmt.executeUpdate(command, Statement.RETURN_GENERATED_KEYS)
    new H2ResultSet(stmt.getGeneratedKeys)
  }

}
