package com.vinctus.oql2

import com.vinctus.oql2.OQL._
import xyz.hyperreal.pretty._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

object SQLQueryBuilder {

  val INDENT = 2

}

class SQLQueryBuilder(val parms: Parameters, oql: String, val margin: Int = 0, subquery: Boolean = false) {

  import SQLQueryBuilder._

  private trait Project
  private case class ValueProject(expr: OQLExpression, table: String) extends Project { override def toString: String = expression(expr, table) }
  private case class QueryProject(query: SQLQueryBuilder) extends Project { override def toString: String = query.toString.trim }

  private var projectQuery: Boolean = false
  private var from: (String, Option[String]) = _
//  private val tables = new mutable.HashMap[String, Int]
  private val innerJoins = new ArrayBuffer[Join]
  private val leftJoins = new mutable.HashSet[Join]
  private var idx = 0
  private val projects = new ArrayBuffer[Project]
  private var where: Option[(String, OQLExpression)] = None
  private var order: Option[(String, List[OQLOrdering])] = None

  def table(name: String, alias: Option[String]): Unit =
    if (from eq null)
      from = (name, alias)

  def select(cond: OQLExpression, table: String): Unit =
    where = where match {
      case Some((_, cur)) =>
        Some((table, InfixOQLExpression(GroupingOQLExpression(cur), "AND", GroupingOQLExpression(cond))))
      case None => Some((table, cond))
    }

  def ordering(orderings: List[OQLOrdering], table: String): Unit = order = Some((table, orderings))

  def projectValue(expr: OQLExpression, table: String): Int = {
    projects += ValueProject(expr, table)
    idx += 1
    idx - 1
  }

  def projectQuery(builder: SQLQueryBuilder): Int = {
    builder.projectQuery = true
    projects += QueryProject(builder)
    idx += 1
    idx - 1
  }

  def expression(expr: OQLExpression, table: String): String =
    expr match {
      case ExistsOQLExpression(query) =>
        val subquery = writeQuery(innerQuery(query), table, Right((parms, margin + 2 * SQLQueryBuilder.INDENT)), oql)
        val sql = subquery.toString

        s"EXISTS (\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case QueryOQLExpression(query) =>
        val subquery = writeQuery(innerQuery(query), table, Right((parms, margin + 2 * SQLQueryBuilder.INDENT)), oql)
        val sql = subquery.toString

        s"(\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case InQueryOQLExpression(left, op, query) =>
        val subquery = writeQuery(innerQuery(query), table, Right((parms, margin + 2 * SQLQueryBuilder.INDENT)), oql)
        val sql = subquery.toString

        s"${expression(left, table)} $op (\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case InParameterOQLExpression(left, op, right @ ParameterOQLExpression(p)) =>
        parms.get(p.s) match {
          case Some(value) if !value.isInstanceOf[Seq[_]] => problem(p.pos, s"parameter '${p.s}' is not an array", oql)
          case _                                          => s"${expression(left, table)} $op ${expression(right, table)}"
        }
      case InArrayOQLExpression(left, op, right) => s"${expression(left, table)} $op (${right map (expression(_, table)) mkString ", "})"
      case ParameterOQLExpression(p) =>
        parms get p.s match {
          case Some(parm: Seq[_]) => parm.mkString("(", ", ", ")")
          case Some(parm: Number) => parm.toString
          case Some(parm: String) => s"'${quote(parm)}'"
          case None               => problem(p.pos, s"parameter '${p.s}' not found", oql)
        }
      case ApplyOQLExpression(f, args)                       => s"${f.s}(${args map (expression(_, table)) mkString ", "})"
      case StarOQLExpression                                 => "*"
      case RawOQLExpression(s)                               => s
      case InfixOQLExpression(left, op @ ("*" | "/"), right) => s"${expression(left, table)}$op${expression(right, table)}"
      case InfixOQLExpression(left, op, right)               => s"${expression(left, table)} $op ${expression(right, table)}"
      case PrefixOQLExpression("-", expr)                    => s"-${expression(expr, table)}"
      case PrefixOQLExpression(op, expr)                     => s"$op ${expression(expr, table)}"
      case PostfixOQLExpression(expr, op)                    => s"${expression(expr, table)} $op"
      case BetweenOQLExpression(expr, op, lower, upper) =>
        s"${expression(expr, table)} $op ${expression(lower, table)} AND ${expression(upper, table)}"
      case GroupingOQLExpression(expr)  => s"(${expression(expr, table)})"
      case FloatOQLExpression(n, pos)   => n.toString
      case IntegerOQLExpression(n, pos) => n.toString
      case LiteralOQLExpression(s, pos) => s"'${quote(s)}'"
      case AttributeOQLExpression(ids, dmrefs) =>
        var alias = table

        dmrefs dropRight 1 foreach {
          case (e: Entity, Attribute(name, _, _, _, _)) =>
            alias = s"$alias$$$name"
            leftJoin(table, name, e.table, alias, e.pk.get.column)
        }

        s"$alias.${dmrefs.last._2.column}"
      case BooleanOQLExpression(b, pos) => b
      case CaseOQLExpression(whens, els) =>
        s"CASE ${whens map {
          case OQLWhen(cond, expr) =>
            s"WHEN ${expression(cond, table)} THEN ${expression(expr, table)}"
        } mkString}${if (els.isDefined) expression(els.get, table) else ""} END"
    }

  def leftJoin(t1: String, c1: String, t2: String, alias: String, c2: String): SQLQueryBuilder = {
    leftJoins += Join(t1, c1, t2, alias, c2)
    this
  }

  def innerJoin(t1: String, c1: String, t2: String, alias: String, c2: String): SQLQueryBuilder = {
    innerJoins += Join(t1, c1, t2, alias, c2)
    this
  }

  private case class Join(t1: String, c1: String, t2: String, alias: String, c2: String)

  override def toString: String = {
    val buf = new StringBuilder
    var indent = margin
    var first = true

    def line(s: String): Unit = {
      if (first && !subquery)
        first = false
      else
        buf ++= " " * indent

      buf ++= s
      buf += '\n'
    }

    def in(): Unit = indent += INDENT

    def out(): Unit = indent -= INDENT

    def sq(yes: String, no: String = "") = if (projectQuery) yes else no

    line(s"${sq("(JSON_ARRAY(")}SELECT ${sq("JSON_ARRAY(")}")
    in()
    in()

    for ((p, i) <- projects.zipWithIndex)
      line(s"$p${if (i < projects.length - 1) "," else sq(" NULL ON NULL)")}")

    out()

    val (tab, alias) = from

    line(s"FROM $tab${if (alias.isDefined) s" AS ${alias.get}" else ""}")
    in()

    val whereClause = where map { case (table, expr) => s"WHERE ${expression(expr, table)}" }
    val orderByClause = order map {
      case (table, orderings) =>
        s"ORDER BY ${orderings map { case OQLOrdering(expr, ordering) => s"${expression(expr, table)} $ordering" } mkString ", "}"
    }

    for (Join(t1, c1, t2, alias, c2) <- innerJoins)
      line(s"JOIN $t2 AS $alias ON $t1.$c1 = $alias.$c2")

    for (Join(t1, c1, t2, alias, c2) <- leftJoins)
      line(s"LEFT JOIN $t2 AS $alias ON $t1.$c1 = $alias.$c2")

    out()
    whereClause foreach line
    orderByClause foreach line

    if (projectQuery)
      line("))")

    out()
    buf.toString
  }

}
