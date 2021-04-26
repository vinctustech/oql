package com.vinctus.oql2

import java.time.Instant
import java.util.UUID

abstract class JDBCDataSource(driver: String) extends SQLDataSource {

  try Class.forName(driver)
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  def url: String

  def user: String

  def password: String

  def timestamp(t: String): Any = Instant.parse(if (t endsWith "Z") t else s"${t}Z")

  def uuid(id: String): Any = UUID.fromString(id)

}
