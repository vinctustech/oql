package com.vinctus.oql2

abstract class JDBCOQLDataSource(driver: String) extends SQLOQLDataSource {

  try Class.forName(driver)
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  def url: String

  def user: String

  def password: String

}
