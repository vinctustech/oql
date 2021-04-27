package com.vinctus.oql2

import com.vinctus.oql2.OQL_TS_NodePG.jsParameters
import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js
import scala.scalajs.js.|
import js.JSConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("OQL")
class OQL_TS_NodePG(dm: String,
                    host: String,
                    port: Int,
                    database: String,
                    user: String,
                    password: String,
                    ssl: Boolean | ConnectionOptions,
                    idleTimeoutMillis: Int,
                    max: Int)
    extends OQL(dm, new PG_NodePG(host, port, database, user, password, ssl, idleTimeoutMillis, max)) {

  @JSExport
  override def showQuery(): Unit = _showQuery = true

  @JSExport("count")
  def jscount(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = count(oql, jsParameters(parameters)).toJSPromise

  @JSExport("queryOne")
  def jsqueryOne(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.UndefOr[Any]] =
    jsqueryOne(parseQuery(oql), oql, parameters)

  def jsqueryOne(query: OQLQuery, oql: String, parameters: js.UndefOr[js.Any]): js.Promise[js.UndefOr[Any]] =
    queryOne(query, oql, () => new JSResultBuilder, jsParameters(parameters)).map(_.orUndefined).toJSPromise

  @JSExport("queryMany")
  def jsqueryMany(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.Array[js.Any]] =
    jsqueryMany(parseQuery(oql), oql, parameters)

  def jsqueryMany(query: OQLQuery, oql: String, parameters: js.UndefOr[js.Any]): js.Promise[js.Array[js.Any]] =
    queryMany(query, oql, () => new JSResultBuilder, jsParameters(parameters)).map(_.arrayResult.asInstanceOf[js.Array[js.Any]]).toJSPromise

  @JSExport("queryBuilder")
  def jsqueryBuilder() = new JSQueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

}

object OQL_TS_NodePG {

  private[oql2] def jsParameters(parameters: js.UndefOr[js.Any]): collection.Map[String, Any] =
    if (parameters.isEmpty) Map()
    else parameters.asInstanceOf[js.Dictionary[Any]]

}
