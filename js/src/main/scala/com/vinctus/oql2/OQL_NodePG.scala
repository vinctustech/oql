package com.vinctus.oql2

import com.vinctus.sjs_utils.{map2cc, Mappable}

import typings.node.tlsMod.ConnectionOptions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
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
    extends AbstractOQL(dm, new NodePG(host, port, database, user, password, ssl, idleTimeoutMillis, max), JSConversions)
    with Dynamic {

  def execute[R](action: OQLConnection => Future[R]): Future[R] = action(connect)

  def entity(name: String): Mutation = new Mutation(this, model.entities(name))

  def selectDynamic(resource: String): Mutation = entity(resource)

  def ccQueryOne[T <: Product: Mappable](oql: String): Future[Option[T]] = queryOne(oql) map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def queryOne(oql: String): Future[Option[Any]] = queryOne(parseQuery(oql), oql)

  def ccQueryMany[T <: Product: Mappable](oql: String): Future[List[T]] = queryMany(oql) map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def queryMany(oql: String): Future[List[Any]] = queryMany(oql, () => new ScalaResultBuilder) map (_.arrayResult.asInstanceOf[List[Any]])

  def queryBuilder() = new QueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

  def json(oql: String, tab: Int = 2, format: Boolean = true): Future[String] =
    queryMany(oql, () => new ScalaResultBuilder) map (r => JSON(r.arrayResult, ds.platformSpecific, tab, format))

  def render(a: Any): String =
    a match {
      case s: String      => s"'${ds.quote(s)}'"
      case d: js.Date     => s"'${d.toISOString()}'"
      case a: js.Array[_] => s"(${a map render mkString ","})"
      case _              => String.valueOf(a)
    }

}
