package com.vinctus.oql2

import xyz.hyperreal.pretty._

import scala.collection.mutable.ArrayBuffer

class SQLQueryBuilder(margin: Int = 0) {

  private var from: String = _
//  private val tables = new mutable.HashMap[String, Int]
  private val innerJoins = new ArrayBuffer[Join]
  private val leftJoins = new ArrayBuffer[Join]
  private var idx = 0
  private val projects = new ArrayBuffer[String]
  private var where: Option[(String, OQLExpression)] = None

  def table(name: String): Unit = {
    if (from eq null)
      from = name
  }

  def select(cond: OQLExpression, table: String): Unit =
    where = where match {
      case Some((_, cur)) =>
        Some((table, InfixOQLExpression(GroupingOQLExpression(cur), "AND", GroupingOQLExpression(cond))))
      case None => Some((table, cond))
    }

  def project(expr: OQLExpression, table: String): Int = {
    projects += expression(expr, table)
    idx += 1
    idx - 1
  }

  def expression(expr: OQLExpression, table: String): String =
    expr match {
      case InfixOQLExpression(left, op @ ("*" | "/"), right) => s"${expression(left, table)}$op${expression(right, table)}"
      case InfixOQLExpression(left, op, right)               => s"${expression(left, table)} $op ${expression(right, table)}"
      case PrefixOQLExpression("-", expr)                    => s"-${expression(expr, table)}"
      case PrefixOQLExpression(op, expr)                     => s"$op ${expression(expr, table)}"
      case PostfixOQLExpression(expr, op)                    => s"${expression(expr, table)} $op"
      case GroupingOQLExpression(expr)                       => s"($expr)"
      case NumberOQLExpression(n, pos)                       => n.toString
      case LiteralOQLExpression(s, pos)                      => s"'${quote(s)}'"
      case AttributeOQLExpression(ids, _, attr)              => s"$table.${attr.column}" //todo ids not being used
    }

  def leftJoin(t1: String, c1: String, t2: String, alias: String, c2: String): SQLQueryBuilder = {
    table(t2)
    leftJoins += Join(t1, c1, t2, alias, c2)
    this
  }

  def innerJoin(t1: String, c1: String, t2: String, alias: String, c2: String): SQLQueryBuilder = {
    innerJoins += Join(t1, c1, t2, alias, c2)
    this
  }

  private case class Join(t1: String, c1: String, t2: String, alias: String, c2: String)

  override def toString: String = {
    val INDENT = 2
    val buf = new StringBuilder
    var indent = margin

    def line(s: String): Unit = {
      buf ++= " " * indent
      buf ++= s
      buf += '\n'
    }

    def in(): Unit = indent += INDENT

    def out(): Unit = indent -= INDENT

    line("SELECT")
    in()
    in()

    for ((p, i) <- projects.zipWithIndex)
      line(s"$p${if (i < projects.length - 1) "," else ""}")

    out()

    line(s"FROM $from")
    in()

    for (Join(t1, c1, t2, alias, c2) <- innerJoins)
      line(s"JOIN $t2 AS $alias ON $t1.$c1 = $alias.$c2")

    for (Join(t1, c1, t2, alias, c2) <- leftJoins)
      line(s"LEFT JOIN $t2 AS $alias ON $t1.$c1 = $alias.$c2")

    out()

    where match {
      case Some((table, expr)) => line(s"WHERE ${expression(expr, table)}")
      case None                =>
    }

    out()

    buf.toString
  }

}
