package com.vinctus.oql

import AbstractOQL.*

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.scalajs.js

object SQLQueryBuilder {

  val INDENT = 2

}

class SQLQueryBuilder(
    oql: String,
    ds: SQLDataSource,
    fixed: Fixed,
    model: DataModel,
    macros: mutable.HashMap[String, Macro],
    val margin: Int = 0,
    subquery: Boolean = false
) {

  import SQLQueryBuilder._

  private var projectQuery: Boolean = false

  def call(f: String, args: String): String = f.replace("?", args)

  private trait Project
  private case class ValueProject(expr: OQLExpression, table: String, typed: Boolean) extends Project {
    override def toString: String = {
      val exp =
        if (projectQuery && ds.convertFunction.isDefined && (expr.typ == UUIDType || expr.typ == TimestampType))
          call(ds.convertFunction.get, expression(expr, table))
        else
          expression(expr, table)

      val typing = if (typed) s", ${call(ds.typeFunction.get, expression(expr, table))}" else ""

      s"$exp$typing"
    }
  }
  private case class QueryProject(query: SQLQueryBuilder) extends Project {
    override def toString: String = query.toString.trim
  }

  private var from: (String, Option[String]) = _
  private val innerJoins = new ArrayBuffer[Join]
  private val leftJoins = new ArrayBuffer[Join]
  private var idx = 0
  private val projects = new ArrayBuffer[Project]
  private var where: Option[(String, OQLExpression)] = None
  private var _group: Option[(String, List[OQLExpression])] = None
  private var _having: Option[(String, OQLExpression)] = None
  private var _order: Option[(String, List[OQLOrdering])] = None
  private var _limit: Option[Int] = None
  private var _offset: Option[Int] = None

  def table(name: String, alias: Option[String]): Unit = if (from eq null) from = (name, alias)

  def select(cond: OQLExpression, table: String): Unit =
    where = where match {
      case Some((_, cur)) =>
        Some((table, InfixOQLExpression(GroupedOQLExpression(cur), "AND", GroupedOQLExpression(cond))))
      case None => Some((table, cond))
    }

  def group(groupings: List[OQLExpression], table: String): Unit = _group = Some((table, groupings))

  def order(orderings: List[OQLOrdering], table: String): Unit = _order = Some((table, orderings))

  def having(expr: OQLExpression, table: String): Unit = _having = Some((table, expr))

  def limit(n: Int): Unit = _limit = Some(n)

  def offset(n: Int): Unit = _offset = Some(n)

  def projectValue(expr: OQLExpression, table: String): (Int, Boolean) = {
    val cur = idx
    val typed = projectQuery && ds.typeFunction.isDefined && expr.typ == null

    projects += ValueProject(expr, table, typed)
    idx += (if (typed) 2 else 1)
    (cur, typed)
  }

  def projectQuery(builder: SQLQueryBuilder): Int = {
    builder.projectQuery = true
    projects += QueryProject(builder)
    idx += 1
    idx - 1
  }

  def expression(expr: OQLExpression, table: String): String = {
    def attribute(dmrefs: List[(Entity, Attribute)]): String = {
      var alias = table

      dmrefs dropRight 1 foreach { case (e: Entity, Attribute(name, column, _, _, _)) =>
        val old_alias = alias

        alias = s"$alias$$$name"
        leftJoin(old_alias, column, e.table, alias, e.pk.get.column)
      }

      s"\"$alias\".\"${dmrefs.last._2.column}\""
    }

    expr match {
      case ExistsOQLExpression(query) =>
        val subquery =
          writeQuery(
            innerQuery(query),
            table,
            Right(margin + 2 * SQLQueryBuilder.INDENT),
            oql,
            ds,
            fixed,
            model,
            macros
          )
        val sql = subquery.toString

        s"EXISTS (\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case QueryOQLExpression(query) =>
        val subquery =
          writeQuery(
            innerQuery(query),
            table,
            Right(margin + 2 * SQLQueryBuilder.INDENT),
            oql,
            ds,
            fixed,
            model,
            macros
          )
        val sql = subquery.toString

        s"(\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case InQueryOQLExpression(left, op, query) =>
        val subquery =
          writeQuery(
            innerQuery(query),
            table,
            Right(margin + 2 * SQLQueryBuilder.INDENT),
            oql,
            ds,
            fixed,
            model,
            macros
          )
        val sql = subquery.toString

        s"${expression(left, table)} $op (\n$sql${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
      case InArrayOQLExpression(left, op, right) =>
        s"${expression(left, table)} $op (${right map (expression(_, table)) mkString ", "})"
      case ApplyOQLExpression(f, args) =>
        val expandedArgs = args map (expression(_, table))

        macros get f.s match
          case None => s"${f.s}(${expandedArgs mkString ", "})"
          case Some(Macro(definition, parameters)) =>
            if parameters.length != args.length then
              problem(f.pos, "number of arguments doesn't equal number of parameters in macro definition", null)

            macroSubstitutionRegex.replaceAllIn(
              definition,
              { m =>
                if m.groupCount == 0 then "$"
                else expandedArgs(parameters.indexOf(m.group(1)))
              }
            )
      case StarOQLExpression   => "*"
      case RawOQLExpression(s) => s
      case InfixOQLExpression(left, op @ ("*" | "/"), right) =>
        s"${expression(left, table)}$op${expression(right, table)}"
      case InfixOQLExpression(left, op, right) => s"${expression(left, table)} $op ${expression(right, table)}"
      case PrefixOQLExpression("-", expr)      => s"-${expression(expr, table)}"
      case PrefixOQLExpression(op, expr)       => s"$op ${expression(expr, table)}"
      case PostfixOQLExpression(expr, op)      => s"${expression(expr, table)} $op"
      case BetweenOQLExpression(expr, op, lower, upper) =>
        s"${expression(expr, table)} $op ${expression(lower, table)} AND ${expression(upper, table)}"
      case GroupedOQLExpression(expr)    => s"(${expression(expr, table)})"
      case TypedOQLExpression(expr, typ) => ds.typed(expression(expr, table), typ)
      case FloatOQLExpression(n)         => n.toString
      case IntegerOQLExpression(n)       => n.toString
      case JSONOQLExpression(e)          => s"'${expression(e, table)}'"
      case ArrayOQLExpression(elems)     => s"[${elems.map(e => expression(e, table)).mkString(", ")}]"
      case ObjectOQLExpression(pairs) =>
        s"{${pairs.map({ case (k, v) => s"\"$k\": ${expression(v, table)}" }).mkString(", ")}}"
      case StringOQLExpression(s)                 => ds.string(s)
      case ReferenceOQLExpression(_, dmrefs)      => attribute(dmrefs)
      case AttributeOQLExpression(List(id), null) => id.s // it's a built-in variable if dmrefs is null
      case AttributeOQLExpression(_, dmrefs)      => attribute(dmrefs)
      case BooleanOQLExpression(b)                => b
      case CaseOQLExpression(whens, els) =>
        s"CASE ${whens map { case OQLWhen(cond, expr) =>
            s"WHEN ${expression(cond, table)} THEN ${expression(expr, table)}"
          } mkString " "}${if (els.isDefined) s" ELSE ${expression(els.get, table)}" else ""} END"
    }
  }

  def leftJoin(t1: String, c1: String, t2: String, alias: String, c2: String): SQLQueryBuilder = {
    if (!leftJoins.exists { case Join(_, _, curt2, curalias, _) => curt2 == t2 && curalias == alias })
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

    def pq(yes: String, no: String = "") = if (projectQuery) yes else no

    line(s"${pq(s"(${ds.resultArrayFunctionStart}")}SELECT ${pq(ds.rowSequenceFunctionStart)}")
    in()
    in()

    for ((p, i) <- projects.zipWithIndex)
      line(s"$p${if (i < projects.length - 1) "," else pq(ds.rowSequenceFunctionEnd)}")

    out()

    val (table, alias) = from

    line(s"FROM \"$table\"${if (alias.isDefined) s" AS \"${alias.get}\"" else ""}")
    in()

    val whereClause = where map { case (table, expr) => s"WHERE ${expression(expr, table)}" }
    val groupByClause =
      _group map { case (table, groupings) =>
        s"GROUP BY ${groupings map (expr => s"${expression(expr, table)}") mkString ", "}"
      }
    val orderByClause =
      _order map { case (table, orderings) =>
        s"ORDER BY ${orderings map { case OQLOrdering(expr, ordering) =>
            s"${expression(expr, table)} $ordering"
          } mkString ", "}"
      }

    for (Join(t1, c1, t2, alias, c2) <- innerJoins)
      line(s"JOIN \"$t2\" AS \"$alias\" ON \"$t1\".\"$c1\" = \"$alias\".\"$c2\"")

    for (Join(t1, c1, t2, alias, c2) <- leftJoins)
      line(s"LEFT JOIN \"$t2\" AS \"$alias\" ON \"$t1\".\"$c1\" = \"$alias\".\"$c2\"")

    out()
    whereClause foreach line
    groupByClause foreach line
    orderByClause foreach line
    _limit foreach (n => line(s"LIMIT $n"))
    _offset foreach (n => line(s"OFFSET $n"))

    if (projectQuery)
      line(s"${ds.resultArrayFunctionEnd})")

    out()
    buf.toString
  }

}
