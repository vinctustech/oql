package com.vinctus.oql2

import java.sql.Statement
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PGJDBCConnection(dataSource: JDBCDataSource) extends JDBCConnection(dataSource) {

  override def query(query: String): Future[OQLResultSet] = Future(new JDBCJSONResultSet(stmt.executeQuery(query)))

  def insert(command: String): Future[JDBCResultSet] =
    Future {
      stmt.executeUpdate(command, Statement.RETURN_GENERATED_KEYS)
      new JDBCJSONResultSet(stmt.getGeneratedKeys)
    }

}
