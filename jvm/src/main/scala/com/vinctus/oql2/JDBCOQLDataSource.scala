package com.vinctus.oql2

import java.sql.{DriverManager, SQLException}

abstract class JDBCOQLDataSource(driver: String) extends OQLDataSource {

  try Class.forName(driver)
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  def databaseURL: String

  def databaseUser: String

  def databasePassword: String

  def mapType(typ: PrimitiveType): String

  def connectJDBC: java.sql.Connection = {
    try DriverManager.getConnection(databaseURL, databaseUser, databasePassword)
    catch { case e: SQLException => sys.error(e.getMessage) }
  }

  def create(model: DataModel): Unit = {}
//    val conn = connectJDBC
//    val stmt = conn.createStatement

//  stmt.execute(

  def schema(model: DataModel): Seq[String] = {
    for (entity <- model.entities.values.toList.sortBy(_.table))
      yield {
        val columns =
          (for (attribute <- entity.attributes.values)
            yield
              s"  ${attribute.column} ${mapType(attribute.typ.asInstanceOf[PrimitiveType])}${if (attribute.pk) " PRIMARY KEY"
              else ""}") mkString ",\n"

        s"""
           |CREATE TABLE ${entity.table} (
           |$columns
           |);""".trim.stripMargin
      }
  }

}
