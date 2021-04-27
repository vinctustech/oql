package com.vinctus.oql2

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

  private def jsParameters(parameters: js.UndefOr[js.Any]): collection.Map[String, Any] =
    if (parameters.isEmpty)
      Map()
    else
      parameters.asInstanceOf[js.Dictionary[Any]]

  @JSExport("count")
  def jscount(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = count(oql, jsParameters(parameters)).toJSPromise

  @JSExport("queryOne")
  def jsqueryOne(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Option[Any]] =
    queryOne(oql, () => new JSResultBuilder, jsParameters(parameters)).toJSPromise

}
