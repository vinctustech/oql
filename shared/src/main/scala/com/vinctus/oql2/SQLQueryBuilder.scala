package com.vinctus.oql2

import xyz.hyperreal.pretty._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SQLQueryBuilder {

  private val tables = new mutable.HashMap[String, Int]
  private val innerJoins = new ArrayBuffer[InnerJoin]
  private val outerJoins = new ArrayBuffer[OuterJoin]

  def table(name: String): Table = {
    tables get name match {
      case Some(a) =>
        val na = a + 1

        tables(name) = na
        new Table(name, na)
      case None =>
        tables(name) = 0
        new Table(name, 0)
    }

  }

  def outerJoin(t1: Table, c1: String, t2: Table, c2: String): Unit = outerJoins += OuterJoin(t1.ref, c1, t2.ref, c2)

  def innerJoin(t1: Table, c1: String, t2: Table, c2: String): Unit = innerJoins += InnerJoin(t1.ref, c1, t2.ref, c2)

  class Table(name: String, alias: Int) {
    def ref: String = if (alias > 0) s"$name$$$alias" else name
  }

  private case class OuterJoin(t1: String, c1: String, t2: String, c2: String)

  private case class InnerJoin(t1: String, c1: String, t2: String, c2: String)

  override def toString: String = {
    val buf = new StringBuilder

    buf.toString
  }

}
