package com.vinctus.oql2

import java.sql.{DriverManager, SQLException}

abstract class JDBCOQLDataSource(driver: String) extends OQLDataSource {

  try Class.forName(driver)
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  def databaseURL: String

  def databaseUser: String

  def databasePassword: String

  def mapType(typ: TypeSpecifier): String

  def connectJDBC: java.sql.Connection = {
    try DriverManager.getConnection(databaseURL, databaseUser, databasePassword)
    catch { case e: SQLException => sys.error(e.getMessage) }
  }

  def create(model: DataModel): Unit = {}
//    val conn = connectJDBC
//    val stmt = conn.createStatement

//  stmt.execute(

  def schema(model: DataModel): Seq[String] = {
    val tables =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield {
          val columns =
            (for (attribute <- entity.attributes.values)
              yield
                s"  ${attribute.column} ${mapType(attribute.typ)}${if (attribute.pk) " PRIMARY KEY"
                else ""}") mkString ",\n"

          s"""
           |CREATE TABLE ${entity.table} (
           |$columns
           |);""".trim.stripMargin
        }

    val foreignKeys =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield {
          for (attribute <- entity.attributes.values if attribute.typ.isInstanceOf[ManyToOneType])
            yield
              s"ALTER TABLE ${entity.table} ADD FOREIGN KEY (${attribute.column}) REFERENCES ${attribute.typ.asInstanceOf[ManyToOneType].entity.table}"
        }

    tables ++ foreignKeys.flatten
  }

}
