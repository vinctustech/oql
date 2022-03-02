package com.vinctus.oql

import com.vinctus.sjs_utils.{DynamicMap, toJS}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.concurrent.Future
import scala.scalajs.js

class OQL_RDB(dm: String, data: String) extends AbstractOQL(dm, new RDBDataSource(data), ScalaConversions) with Dynamic {

  def execute[R](action: OQLConnection => Future[R]): Future[R] = action(connect)

  def entity(name: String): Mutation = new Mutation(this, model.entities(name))

  def selectDynamic(resource: String): Mutation = entity(resource)

  def jsQueryOne[T <: js.Object](oql: String): Future[Option[T]] = queryOne(oql) map (_.map(toJS(_).asInstanceOf[T]))

  def jsQueryOne[T <: js.Object](q: OQLQuery, fixed: String = null, at: Any = null): Future[Option[T]] =
    queryOne(q, "", fixedEntity(fixed, at)) map (_.map(toJS(_).asInstanceOf[T]))

//  def ccQueryOne[T <: Product: Mappable](oql: String): Future[Option[T]] = queryOne(oql) map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def queryOne(oql: String, fixed: String = null, at: Any = null): Future[Option[DynamicMap]] = queryOne(parseQuery(oql), oql, fixedEntity(fixed, at))

  def jsQueryMany[T <: js.Object](oql: String): Future[T] = (queryMany(oql) map (toJS(_))).asInstanceOf[Future[T]]

  def jsQueryMany[T <: js.Object](q: OQLQuery): Future[T] =
    (queryMany(q, "", () => new ScalaPlainResultBuilder, Fixed(operative = false)) map (toJS(_))).asInstanceOf[Future[T]]

//  def ccQueryMany[T <: Product: Mappable](oql: String): Future[List[T]] = queryMany(oql) map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def queryMany(oql: String): Future[List[DynamicMap]] =
    queryMany(oql, () => new ScalaJSResultBuilder, Fixed(operative = false)) map (_.arrayResult.asInstanceOf[List[DynamicMap]])

//  def queryBuilder() = new SJSQueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

  def json(oql: String, tab: Int = 2, format: Boolean = true): Future[String] =
    queryMany(oql, () => new ScalaPlainResultBuilder, Fixed(operative = false)) map (r => JSON(r.arrayResult, ds.platformSpecific, tab, format))

  def render(a: Any, typ: Option[Datatype] = None): String =
    if (typ.isDefined)
      ds.typed(a, typ.get)
    else
      a match {
        case s: String      => ds.string(s)
        case d: js.Date     => s"'${d.toISOString()}'"
        case a: js.Array[_] => s"(${a map (e => render(e, typ)) mkString ","})"
        case _              => String.valueOf(a)
      }

}
