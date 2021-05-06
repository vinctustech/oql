package com.vinctus.oql2

import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js
import scala.scalajs.js.|
import js.JSConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.matching.Regex

@JSExportTopLevel("OQL2")
class OQL_NodePG(dm: String,
                 host: String,
                 port: Int,
                 database: String,
                 user: String,
                 password: String,
                 ssl: Boolean | ConnectionOptions,
                 idleTimeoutMillis: Int,
                 max: Int)
    extends OQL(dm, new PG_NodePG(host, port, database, user, password, ssl, idleTimeoutMillis, max), JSResultBuilderFactory) {

  import OQL_NodePG._

//  def mutate(command: String): Any

  override def execute[R](action: OQLConnection => Future[R]): Future[R] = action(connect)

  @JSExport
  override def showQuery(): Unit = _showQuery = true

  @JSExport("count")
  def jscount(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = count(substitute(oql, parameters)).toJSPromise

  @JSExport("queryOne")
  def jsqueryOne(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.UndefOr[Any]] = {
    val subst = substitute(oql, parameters)

    jsqueryOne(parseQuery(subst), subst)
  }

  def jsqueryOne(query: OQLQuery, oql: String): js.Promise[js.UndefOr[Any]] =
    jsqueryMany(query, oql).toFuture map {
      case a if a.length == 0 => js.undefined
      case a if a.length == 1 => a.head
      case _                  => sys.error(s"queryOne: more than one row was found")
    } toJSPromise

  @JSExport("queryMany")
  def jsqueryMany(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.Array[js.Any]] = {
    val subst = substitute(oql, parameters)

    jsqueryMany(parseQuery(subst), subst)
  }

  def jsqueryMany(query: OQLQuery, oql: String): js.Promise[js.Array[js.Any]] =
    queryMany(query, oql, () => new JSResultBuilder).map(_.arrayResult.asInstanceOf[js.Array[js.Any]]).toJSPromise

  @JSExport("queryBuilder")
  def jsqueryBuilder() = new JSQueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

  @JSExport
  def raw(sql: String, values: js.UndefOr[js.Array[js.Any]]): js.Promise[js.Array[js.Any]] =
    ds.asInstanceOf[PG_NodePG]
      .connect
      .raw(sql, if (values.isEmpty) js.Array() else values.get)

}

object OQL_NodePG {

  private val varRegex = ":([a-zA-Z_][a-zA-Z0-9_]*)" r
  private val specialRegex = """(['\\\r\n])""" r

  def substitute(s: String, parameters: js.UndefOr[js.Any]): String = // todo: unit tests for parameters
    if (parameters.isEmpty) s
    else
      varRegex.replaceAllIn(
        s,
        m =>
          parameters.asInstanceOf[js.Dictionary[Any]] get m.group(1) match {
            case None        => sys.error(s"template: parameter '${m.group(1)}' not found")
            case Some(value) => Regex.quoteReplacement(render(value))
        }
      )

  def render(a: Any): String =
    a match {
      case s: String      => s"'${quote(s)}'"
      case d: js.Date     => s"'${d.toISOString()}'"
      case a: js.Array[_] => s"(${a map render mkString ","})"
      case _              => String.valueOf(a)
    }

  def quote(s: String): String =
    specialRegex.replaceAllIn(s, _.group(1) match {
      case "'"  => "''"
      case "\\" => """\\\\"""
      case "\r" => """\\r"""
      case "\n" => """\\n"""
    })

}
