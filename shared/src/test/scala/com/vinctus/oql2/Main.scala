package com.vinctus.oql2

import xyz.hyperreal.pretty._

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

object Main extends App {

  import java.sql.DriverManager
  import java.sql.SQLException

  try Class.forName("org.h2.Driver")
  catch { case e: ClassNotFoundException => sys.error(e.getMessage) }

  val conn = connect

  try {
    val stmt = conn.createStatement

    stmt.execute("CREATE TABLE PERSON(id int primary key, name text)")
    stmt.execute("INSERT INTO PERSON(id, name) VALUES(1, 'Anju')")
    stmt.execute("INSERT INTO PERSON(id, name) VALUES(2, 'Sonia')")
    stmt.execute("INSERT INTO PERSON(id, name) VALUES(3, 'Asha')")

    val rs = stmt.executeQuery("select * from PERSON")

    println("H2 In-Memory Database inserted through Statement")

    while (rs.next) println("Id " + rs.getInt("id") + " Name " + rs.getString("name"))

    stmt.close()
  } catch {
    case e: SQLException =>
      println("Exception Message " + e.getLocalizedMessage)
    case e: Exception =>
      e.printStackTrace()
  } finally conn.close()

  def connect =
    try DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "")
    catch { case e: SQLException => sys.error(e.getMessage) }

}

//  val input = "entity a (b) { x: int } entity b { y: int }"
//  val dml = DMLParse(input)
//
//  println(prettyPrint(dml))
//
//  val model = new DataModel(dml.get, input)
