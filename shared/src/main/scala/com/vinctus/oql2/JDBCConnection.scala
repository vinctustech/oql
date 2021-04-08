package com.vinctus.oql2

import java.sql.{DriverManager, SQLException}

abstract class JDBCConnection(val dataSource: JDBCDataSource) extends OQLConnection {

  private[oql2] val conn =
    try DriverManager.getConnection(dataSource.url, dataSource.user, dataSource.password)
    catch { case e: SQLException => sys.error(e.getMessage) }
  private[oql2] val stmt = conn.createStatement

  def query(query: String): OQLResultSet = new JDBCResultSet(stmt.executeQuery(query))

  def execute(command: String): Unit = stmt.executeUpdate(command)

  def create(model: DataModel): Unit = dataSource.schema(model) foreach execute

  def close(): Unit = {
    stmt.close()
    conn.close()
  }

}
