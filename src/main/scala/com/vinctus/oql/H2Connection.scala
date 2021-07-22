package com.vinctus.oql

import java.sql.Statement
import scala.concurrent.Future

class H2Connection(dataSource: JDBCDataSource)(implicit ec: scala.concurrent.ExecutionContext) extends JDBCConnection(dataSource) {

  override def command(query: String): Future[OQLResultSet] =
    Future(new JDBCJSONResultSet(stmt.executeQuery(query)))

  def insert(command: String): Future[JDBCResultSet] =
    Future {
      stmt.executeUpdate(command, Statement.RETURN_GENERATED_KEYS)
      new JDBCJSONResultSet(stmt.getGeneratedKeys)
    }

}
