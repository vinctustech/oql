package com.vinctus.oql2

import java.sql.{DriverManager, SQLException}

abstract class JDBCOQLDataSource(driver: String) extends OQLDataSource {

  try Class.forName(driver)
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  def url: String

  def user: String

  def password: String

  def mapType(typ: TypeSpecifier): String

  def mapPKType(typ: PrimitiveType): String

  def connect: OQLConnection = new JDBCOQLConnection(this)

  def create(model: DataModel): Unit = {
    val conn = connect

    schema(model) foreach conn.execute
    conn.close()
  }

  def schema(model: DataModel): Seq[String] = {
    val tables =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield {
          val columns =
            for (attribute <- entity.attributes.values)
              yield
                if (attribute.pk)
                  s"  ${attribute.column} ${mapPKType(attribute.typ.asInstanceOf[PrimitiveType])} PRIMARY KEY"
                else
                  s"  ${attribute.column} ${mapType(attribute.typ)}${if (attribute.required) " NOT NULL" else ""}"

          s"""
           |CREATE TABLE ${entity.table} (
           |${columns mkString ",\n"}
           |);""".trim.stripMargin
        }

    val foreignKeys =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield
          for (attribute <- entity.attributes.values if attribute.typ.isInstanceOf[ManyToOneType])
            yield
              s"ALTER TABLE ${entity.table} ADD FOREIGN KEY (${attribute.column}) REFERENCES ${attribute.typ.asInstanceOf[ManyToOneType].entity.table};"

    tables ++ foreignKeys.flatten
  }

}

class JDBCOQLConnection(val dataSource: JDBCOQLDataSource) extends OQLConnection {

  private val conn =
    try DriverManager.getConnection(dataSource.url, dataSource.user, dataSource.password)
    catch { case e: SQLException => sys.error(e.getMessage) }
  private val stmt = conn.createStatement

  def query(query: String): OQLResultSet = new JDBCOQLResultSet(stmt.executeQuery(query))

  def execute(command: String): Unit = stmt.executeUpdate(command)

  def close(): Unit = {
    stmt.close()
    conn.close()
  }

}
