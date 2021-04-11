package com.vinctus.oql2

import xyz.hyperreal.pretty._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SQLQueryBuilder(margin: Int = 0) {

  private var from: String = null
  private val tables = new mutable.HashMap[String, Int]
  private val innerJoins = new ArrayBuffer[Join]
  private val leftJoins = new ArrayBuffer[Join]
  private val projects = new ArrayBuffer[OQLExpression]
  private var select: Option[OQLExpression] = None

  def table(name: String): String = {
    if (from eq null)
      from = name

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

  def project(expr: OQLExpression): SQLQueryBuilder = {
    projects += expr
    this
  }

  def expression(expr: OQLExpression): String =
    expr match {
      case InfixOQLExpression(left, op @ ("*" | "/"), right) => s"${expression(left)}$op${expression(right)}"
      case InfixOQLExpression(left, op, right)               => s"${expression(left)} $op ${expression(right)}"
      case PrefixOQLExpression("-", expr)                    => s"-${expression(expr)}"
      case PrefixOQLExpression(op, expr)                     => s"$op ${expression(expr)}"
      case PostfixOQLExpression(expr, op)                    => s"${expression(expr)} $op"
      case GroupingOQLExpression(expr)                       => s"($expr)"
      case NumberOQLExpression(n, pos)                       => n.toString
      case LiteralOQLExpression(s, pos)                      => s"'${quote(s)}'"
      case AttributeOQLExpression(ids, _, column)            => column
    }

  def leftJoin(t1: String, c1: String, t2: String, c2: String): SQLQueryBuilder = {
    table(t2)
    leftJoins += Join(t1, c1, t2, c2)
    this
  }

  def innerJoin(t1: String, c1: String, t2: String, c2: String): SQLQueryBuilder = {
    innerJoins += Join(t1, c1, t2, c2)
    this
  }

  private case class Join(t1: String, c1: String, t2: String, c2: String)

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

    line(s"SELECT ${expression(projects.head)}${if (projects.tail.nonEmpty) "," else ""}")
    indent += 7

    val plen = projects.tail.length

    for ((p, i) <- projects.tail.zipWithIndex)
      line(s"${expression(p)}${if (i < plen - 1) "," else ""}")

    indent -= 7
    in()

//    val froms = tables.toList.flatMap { case (t, a) => t +: ((1 to a) map (i => s"$t AS $t$$$i")) }

    line(s"FROM $from")
    in()

    for (Join(t1, c1, t2, c2) <- innerJoins)
      line(s"JOIN $t2 ON $t1.$c1 = $t2.$c2")

    for (Join(t1, c1, t2, c2) <- leftJoins)
      line(s"LEFT JOIN $t2 ON $t1.$c1 = $t2.$c2")

    out()

    select match {
      case Some(expr) =>
        in()
        line(s"WHERE ${expression(expr)}")
        out()
      case None =>
    }

    out()

    buf.toString
  }

}
