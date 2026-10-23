package com.vinctus.oql

import AbstractOQL._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.scalajs.js
import scala.compiletime.uninitialized

object SQLQueryBuilder {

  val INDENT = 2

}

class SQLQueryBuilder(
    oql: String,
    ds: SQLDataSource,
    fixed: Fixed,
    model: DataModel,
    val margin: Int = 0,
    subquery: Boolean = false,
    // The (alias, builder) of each query enclosing this one, innermost first; outer references render
    // against these aliases and add their joins to these builders.
    val outer: List[(String, SQLQueryBuilder)] = Nil,
) {

  import SQLQueryBuilder._

  private var projectQuery: Boolean = false

  def call(f: String, args: String): String = f.replace("?", args)

  private trait Project
  private case class ValueProject(
      expr: OQLExpression,
      table: String,
      typed: Boolean,
      scopes: List[(String, SQLQueryBuilder)],
  ) extends Project {
    override def toString: String = {
      val exp =
        if (projectQuery && ds.convertFunction.isDefined && (expr.typ == UUIDType || expr.typ == TimestampType))
          call(ds.convertFunction.get, expression(expr, table, scopes))
        else
          expression(expr, table, scopes)

      val typing = if (typed) s", ${call(ds.typeFunction.get, expression(expr, table, scopes))}" else ""

      s"$exp$typing"
    }
  }
  private case class QueryProject(query: SQLQueryBuilder) extends Project {
    override def toString: String = query.toString.trim
  }

  private var from: (String, Option[String])                = uninitialized
  private val innerJoins                                    = new ArrayBuffer[Join]
  private val leftJoins                                     = new ArrayBuffer[Join]
  private var idx                                           = 0
  private val projects                                      = new ArrayBuffer[Project]
  private var where: Option[(String, OQLExpression)]        = None
  private var _group: Option[(String, List[OQLExpression])] = None
  private var _order: Option[(String, List[OQLOrdering])]   = None
  private var _limit: Option[Int]                                      = None
  private var _offset: Option[Int]                                     = None
  private var _distinct: Boolean                                       = false
  private var _distinctOn: Option[(String, List[OQLExpression])]       = None
  private var _having: Option[(String, OQLExpression)]                 = None

  def table(name: String, alias: Option[String]): Unit = if (from eq null) from = (name, alias)

  def distinct(d: Boolean): Unit = _distinct = d

  def distinctOn(exprs: List[OQLExpression], table: String): Unit = _distinctOn = Some((table, exprs))

  def having(cond: OQLExpression, table: String): Unit = _having = Some((table, cond))

  def select(cond: OQLExpression, table: String): Unit =
    where = where match {
      case Some((_, cur)) =>
        Some((table, InfixOQLExpression(GroupedOQLExpression(cur), "AND", GroupedOQLExpression(cond))))
      case None => Some((table, cond))
    }

  def group(groupings: List[OQLExpression], table: String): Unit = _group = Some((table, groupings))

  def order(orderings: List[OQLOrdering], table: String): Unit = _order = Some((table, orderings))

  def limit(n: Int): Unit = _limit = Some(n)

  def offset(n: Int): Unit = _offset = Some(n)

  def projectValue(
      expr: OQLExpression,
      table: String,
      scopes: List[(String, SQLQueryBuilder)] = outer,
  ): (Int, Boolean) = {
    val cur   = idx
    val typed = !subquery && ds.typeFunction.isDefined && expr.typ == null

    projects += ValueProject(expr, table, typed, scopes)
    idx += (if (typed) 2 else 1)
    (cur, typed)
  }

  def projectQuery(builder: SQLQueryBuilder): Int = {
    builder.projectQuery = true
    projects += QueryProject(builder)
    idx += 1
    idx - 1
  }

  def expression(expr: OQLExpression, table: String, scopes: List[(String, SQLQueryBuilder)] = outer): String = {
    def attribute(from: String, dmrefs: List[(Entity, Attribute)], builder: SQLQueryBuilder): String = {
      var alias = from

      dmrefs dropRight 1 foreach { case (e: Entity, Attribute(name, column, _, _, _)) =>
        val old_alias = alias

        alias = s"$alias$$$name"
        builder.leftJoin(old_alias, column, e.table, alias, e.pk.get.column)
      }

      s"\"$alias\".\"${dmrefs.last._2.column}\""
    }

    def subquery(query: OQLQuery): String =
      writeQuery(
        innerQuery(query),
        table,
        Right(margin + 2 * SQLQueryBuilder.INDENT),
        oql,
        ds,
        fixed,
        model,
        scopes,
        this,
      ).toString

    def render(expr: OQLExpression): String =
      expr match {
        case ExistsOQLExpression(query) =>
          s"EXISTS (\n${subquery(query)}${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
        case QueryOQLExpression(query) =>
          s"(\n${subquery(query)}${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
        case InQueryOQLExpression(left, op, query) =>
          s"${render(left)} $op (\n${subquery(query)}${" " * (margin + 2 * SQLQueryBuilder.INDENT)})"
        case InArrayOQLExpression(_, op, Nil) => if (op == "NOT IN") "TRUE" else "FALSE"
        case InArrayOQLExpression(left, op, right) =>
          s"${render(left)} $op (${right map render mkString ", "})"
        case ArrayComparisonOQLExpression(left, op, quantifier, array) =>
          s"${render(left)} $op $quantifier(${render(array)})"
        case ApplyOQLExpression(f, args)                       => s"${f.s}(${args map render mkString ", "})"
        case StarOQLExpression                                 => "*"
        case RawOQLExpression(s)                               => s
        case InfixOQLExpression(left, op @ ("*" | "/"), right) => s"${render(left)}$op${render(right)}"
        case InfixOQLExpression(left, op, right)               => s"${render(left)} $op ${render(right)}"
        case PrefixOQLExpression("-", expr)                    => s"-${render(expr)}"
        case PrefixOQLExpression(op, expr)                     => s"$op ${render(expr)}"
        case PostfixOQLExpression(expr, op)                    => s"${render(expr)} $op"
        case BetweenOQLExpression(expr, op, lower, upper) =>
          s"${render(expr)} $op ${render(lower)} AND ${render(upper)}"
        case OverlapsOQLExpression(leftStart, leftEnd, rightStart, rightEnd) =>
          s"(${render(leftStart)}, ${render(leftEnd)}) OVERLAPS (${render(rightStart)}, ${render(rightEnd)})"
        case GroupedOQLExpression(expr)    => s"(${render(expr)})"
        case TypedOQLExpression(expr, typ) => ds.typed(render(expr), typ)
        case FloatOQLExpression(n)         => n.toString
        case IntegerOQLExpression(n)       => n.toString
        case JSONOQLExpression(e)          => s"'${render(e)}'"
        case ArrayOQLExpression(elems)     => s"[${elems.map(render).mkString(", ")}]"
        case ObjectOQLExpression(pairs)    =>
          s"{${pairs.map({ case (k, v) => s"\"$k\": ${render(v)}" }).mkString(", ")}}"
        case StringOQLExpression(s)            => ds.string(s)
        case ReferenceOQLExpression(_, dmrefs) => attribute(table, dmrefs, this)
        case OuterOQLExpression(_, level, dmrefs) =>
          val (alias, builder) = scopes(level)

          attribute(alias, dmrefs, builder)
        case AttributeOQLExpression(List(id), null) => id.s // it's a built-in variable if dmrefs is null
        case AttributeOQLExpression(_, dmrefs)      => attribute(table, dmrefs, this)
        case BooleanOQLExpression(b)                => b
        case CaseOQLExpression(whens, els)          =>
          s"CASE ${whens map { case OQLWhen(cond, expr) =>
              s"WHEN ${render(cond)} THEN ${render(expr)}"
            } mkString " "}${if (els.isDefined) s" ELSE ${render(els.get)}" else ""} END"
      }

    render(expr)
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
    val buf    = new StringBuilder
    var indent = margin
    var first  = true

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

    val distinctClause = _distinctOn match {
      case Some((table, exprs)) =>
        s"DISTINCT ON (${exprs.map(e => expression(e, table)).mkString(", ")}) "
      case None =>
        if (_distinct) "DISTINCT " else ""
    }
    line(s"${pq(s"(${ds.resultArrayFunctionStart}")}SELECT ${distinctClause}${pq(ds.rowSequenceFunctionStart)}")
    in()
    in()

    for ((p, i) <- projects.zipWithIndex)
      line(s"$p${if (i < projects.length - 1) "," else pq(ds.rowSequenceFunctionEnd)}")

    out()

    val (table, alias) = from

    line(s"FROM \"$table\"${if (alias.isDefined) s" AS \"${alias.get}\"" else ""}")
    in()

    val whereClause   = where map { case (table, expr) => s"WHERE ${expression(expr, table)}" }
    val groupByClause =
      _group map { case (table, groupings) =>
        s"GROUP BY ${groupings map (expr => s"${expression(expr, table)}") mkString ", "}"
      }
    val havingClause =
      _having map { case (table, expr) => s"HAVING ${expression(expr, table)}" }
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
    havingClause foreach line
    orderByClause foreach line
    _limit foreach (n => line(s"LIMIT $n"))
    _offset foreach (n => line(s"OFFSET $n"))

    if (projectQuery)
      line(s"${ds.resultArrayFunctionEnd})")

    out()
    buf.toString
  }

}
