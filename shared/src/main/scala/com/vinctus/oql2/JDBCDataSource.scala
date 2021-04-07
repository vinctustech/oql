package com.vinctus.oql2

abstract class JDBCDataSource(driver: String) extends SQLDataSource {

  try Class.forName(driver)
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  def url: String

  def user: String

  def password: String

}
