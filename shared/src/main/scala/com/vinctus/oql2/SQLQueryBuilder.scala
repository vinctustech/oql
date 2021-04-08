package com.vinctus.oql2

import xyz.hyperreal.pretty._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SQLQueryBuilder(margin: Int = 0) {

  private val tables = new mutable.HashMap[String, Int]
  private val innerJoins = new ArrayBuffer[Join]
  private val outerJoins = new ArrayBuffer[Join]

  def table(name: String): String = {
    tables get name match {
      case Some(a) =>
        val alias = a + 1

        tables(name) = alias
        s"$name$$$alias"
      case None =>
        tables(name) = 0
        name
    }
  }

  def ref(tab: String, col: String): String = s"$tab.$col"

  def expression(expr: OQLExpression): String =
    expr match {
      case InfixOQLExpression(left, op @ ("+" | "-"), right) => s"${expression(left)} $op ${expression(right)}"
      case InfixOQLExpression(left, op, right)               => s"${expression(left)}$op${expression(right)}"
      case GroupingOQLExpression(expr)                       => s"($expr)"
      case NumberOQLExpression(n, pos)                       => n.toString
      case LiteralOQLExpression(s, pos)                      => s"'${quote(s)}'"
      case AttributeOQLExpression(ids, _, column)            => column
    }

  def outerJoin(t1: String, c1: String, t2: String, c2: String): Unit = outerJoins += Join(t1, c1, t2, c2)

  def innerJoin(t1: String, c1: String, t2: String, c2: String): Unit = innerJoins += Join(t1, c1, t2, c2)

  private case class Join(t1: String, c1: String, t2: String, c2: String)

  override def toString: String = {
    val buf = new StringBuilder

    buf.toString
  }

}
