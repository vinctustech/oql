package com.vinctus.oql

import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js
import scala.scalajs.js.|
import js.JSConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.matching.Regex

@JSExportTopLevel("OQL")
class OQL_NodePG_JS(dm: String,
                    host: String,
                    port: Int,
                    database: String,
                    user: String,
                    password: String,
                    ssl: Boolean | ConnectionOptions,
                    idleTimeoutMillis: Int,
                    max: Int)
    extends AbstractOQL(dm, new NodePG(host, port, database, user, password, ssl, idleTimeoutMillis, max), JSConversions) {

  def execute[R](action: OQLConnection => Future[R]): Future[R] = action(connect)

  @JSExport
  def entity(name: String): JSMutation = new JSMutation(this, model.entities(name))

  @JSExport("showQuery")
  def jsshowQuery(): Unit = showQuery()

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
    ds.asInstanceOf[NodePG]
      .connect
      .raw(sql, if (values.isEmpty) js.Array() else values.get)

  private val varRegex = ":([a-zA-Z_][a-zA-Z0-9_]*)" r

  def substitute(s: String, parameters: js.UndefOr[js.Any]): String = // todo: unit tests for parameters
    if (parameters.isEmpty) s
    else
      varRegex.replaceAllIn(
        s,
        m =>
          parameters.asInstanceOf[js.Dictionary[Any]] get m.group(1) match {
            case None        => sys.error(s"template: parameter '${m.group(1)}' not found")
            case Some(value) => Regex.quoteReplacement(subsrender(value))
        }
      )

  def subsrender(a: Any): String =
    a match {
      case s: String      => s"'${s.replace("'", "\\'")}'"
      case d: js.Date     => s"'${d.toISOString()}'"
      case a: js.Array[_] => s"(${a map render mkString ","})"
      case _              => String.valueOf(a)
    }

  def render(a: Any): String =
    a match {
      case s: String      => s"'${ds.quote(s)}'"
      case d: js.Date     => s"'${d.toISOString()}'"
      case a: js.Array[_] => s"(${a map render mkString ","})"
      case _              => String.valueOf(a)
    }

}

//todo: investigate 'subsrender' thoroughly
