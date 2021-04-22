package com.vinctus.oql2

import com.vinctus.oql2.OQL._
import org.graalvm.compiler.debug.TTY.out
import xyz.hyperreal.pretty._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

object SQLQueryBuilder {

  val INDENT = 2
  val Q = '"'

}

class SQLQueryBuilder(val parms: Parameters, oql: String, ds: SQLDataSource, val margin: Int = 0, subquery: Boolean = false) {

  import SQLQueryBuilder._

  private var projectQuery: Boolean = false

  def call(f: String, args: String): String = f.replace("?", args)

  private trait Project
  private case class ValueProject(expr: OQLExpression, table: String) extends Project {
    override def toString: String = {
      val exp =
        if (projectQuery && ds.convertFunction.isDefined && (expr.typ == UUIDType || expr.typ == TimestampType))
          call(ds.convertFunction.get, expression(expr, table))
        else
          expression(expr, table)

      val typing =
        if (projectQuery && ds.typeFunction.isDefined) s", ${ds.typeFunction.get}(${expression(expr, table)})"
        else ""

      s"$exp$typing"
    }
  }
  private case class QueryProject(query: SQLQueryBuilder) extends Project { override def toString: String = query.toString.trim }

  private var from: (String, Option[String]) = _
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
        Some((table, InfixOQLExpression(GroupedOQLExpression(cur), "AND", GroupedOQLExpression(cond))))
      case None => Some((table, cond))
    }

  def ordering(orderings: List[OQLOrdering], table: String): Unit = order = Some((table, orderings))

  def projectValue(expr: OQLExpression, table: String): (Int, Boolean) = {
    projects += ValueProject(expr, table)

    val cur = idx

    idx += (if (projectQuery) 2 else 1)
    (cur, projectQuery)
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
        val subquery = writeQuery(innerQuery(query), table, Right((parms, margin + 2 * SQLQueryBuilder.INDENT)), oql, ds)
        val sql = subquery.toString

        s"EXISTS (\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case QueryOQLExpression(query) =>
        val subquery = writeQuery(innerQuery(query), table, Right((parms, margin + 2 * SQLQueryBuilder.INDENT)), oql, ds)
        val sql = subquery.toString

        s"(\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case InQueryOQLExpression(left, op, query) =>
        val subquery = writeQuery(innerQuery(query), table, Right((parms, margin + 2 * SQLQueryBuilder.INDENT)), oql, ds)
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
      case GroupedOQLExpression(expr)   => s"(${expression(expr, table)})"
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

        s"$Q$alias$Q.$Q${dmrefs.last._2.column}$Q"
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

    line(s"${sq(s"(${ds.resultArrayFunctionStart}")}SELECT ${sq(ds.rowSequenceFunctionStart)}")
    in()
    in()

    for ((p, i) <- projects.zipWithIndex)
      line(s"$p${if (i < projects.length - 1) "," else sq(ds.rowSequenceFunctionEnd)}") //sq(" NULL ON NULL)")

    out()

    val (table, alias) = from

    line(s"FROM $Q$table$Q${if (alias.isDefined) s" AS $Q${alias.get}$Q" else ""}")
    in()

    val whereClause = where map { case (table, expr) => s"WHERE ${expression(expr, table)}" }
    val orderByClause = order map {
      case (table, orderings) =>
        s"ORDER BY ${orderings map { case OQLOrdering(expr, ordering) => s"${expression(expr, table)} $ordering" } mkString ", "}"
    }

    for (Join(t1, c1, t2, alias, c2) <- innerJoins)
      line(s"JOIN $Q$t2$Q AS $Q$alias$Q ON $Q$t1$Q.$Q$c1$Q = $Q$alias$Q.$Q$c2$Q")

    for (Join(t1, c1, t2, alias, c2) <- leftJoins)
      line(s"LEFT JOIN $Q$t2$Q AS $Q$alias$Q ON $Q$t1$Q.$Q$c1$Q = $Q$alias$Q.$Q$c2$Q")

    out()
    whereClause foreach line
    orderByClause foreach line

    if (projectQuery)
      line(s"${ds.resultArrayFunctionEnd})")

    out()
    buf.toString
  }

}
