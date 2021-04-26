package com.vinctus.oql2

import java.sql.{DriverManager, SQLException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class JDBCConnection(val dataSource: JDBCDataSource) extends OQLConnection {

  private[oql2] val conn =
    try DriverManager.getConnection(dataSource.url, dataSource.user, dataSource.password)
    catch { case e: SQLException => sys.error(e.getMessage) }
  private[oql2] val stmt = conn.createStatement

  def query(query: String): Future[OQLResultSet] = Future(new JDBCResultSet(stmt.executeQuery(query)) with ArrayResultSet)

  def execute(command: String): Future[Unit] = Future(stmt.executeUpdate(command))

  def create(model: DataModel): Future[Unit] = Future(dataSource.schema(model) foreach execute)

  def close(): Unit = {
    stmt.close()
    conn.close()
  }

}
