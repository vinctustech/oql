package com.vinctus.oql2

import typings.node.tlsMod.ConnectionOptions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.|

class OQL_NodePG(dm: String,
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

  def queryOne(oql: String): Future[Option[Any]] = queryOne(parseQuery(oql), oql)

  def queryBuilder() = new QueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

  def json(oql: String, tab: Int = 2, format: Boolean = true): Future[String] =
    queryMany(oql, () => new ScalaResultBuilder) map (r => JSON(r.arrayResult, ds.platformSpecific, tab, format))

}
