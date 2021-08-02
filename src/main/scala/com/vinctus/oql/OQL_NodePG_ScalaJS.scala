package com.vinctus.oql

import com.vinctus.sjs_utils.{DynamicMap, toJS}
import com.vinctus.mappable.{Mappable, map2cc}
import typings.node.tlsMod.ConnectionOptions

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.|
import scala.util.matching.Regex

class OQL_NodePG_ScalaJS(dm: String,
                         host: String,
                         port: Int,
                         database: String,
                         user: String,
                         password: String,
                         ssl: Boolean | ConnectionOptions,
                         idleTimeoutMillis: Int,
                         max: Int)(implicit ec: scala.concurrent.ExecutionContext)
    extends AbstractOQL(dm, new NodePG(host, port, database, user, password, ssl, idleTimeoutMillis, max), ScalaConversions)
    with Dynamic {

  def execute[R](action: OQLConnection => Future[R]): Future[R] = action(connect)

  def entity(name: String): Mutation = new Mutation(this, model.entities(name))

  def selectDynamic(resource: String): Mutation = entity(resource)

  def jsQueryOne[T <: js.Object](oql: String, fixed: String = null, at: Any = null): Future[Option[T]] =
    queryOne(oql, fixed, at) map (_.map(toJS(_).asInstanceOf[T]))

  def jsQueryOne[T <: js.Object](q: OQLQuery, fixed: String = null, at: Any = null): Future[Option[T]] =
    queryOne(q, "", fixedEntity(fixed, at)) map (_.map(toJS(_).asInstanceOf[T]))

  def ccQueryOne[T <: Product: Mappable](oql: String, fixed: String = null, at: Any = null): Future[Option[T]] =
    queryOne(oql, fixed, at) map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def queryOne(oql: String, fixed: String = null, at: Any = null): Future[Option[DynamicMap]] = queryOne(parseQuery(oql), oql, fixedEntity(fixed, at))

  def jsQueryMany[T <: js.Object](oql: String, fixed: String = null, at: Any = null): Future[T] =
    (queryMany(oql, fixed, at) map (toJS(_))).asInstanceOf[Future[T]]

  def jsQueryMany[T <: js.Object](q: OQLQuery): Future[T] =
    (queryMany(q, "", () => new ScalaPlainResultBuilder, Fixed(operative = false)) map (toJS(_))).asInstanceOf[Future[T]]

  def ccQueryMany[T <: Product: Mappable](oql: String, fixed: String = null, at: Any = null): Future[List[T]] =
    queryMany(oql, fixed, at) map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def queryMany(oql: String, fixed: String = null, at: Any = null, parameters: Map[String, Any] = Map()): Future[List[DynamicMap]] = {
    val subst = substitute(oql, parameters)

    queryMany(subst, () => new ScalaJSResultBuilder, fixedEntity(fixed, at)) map (_.arrayResult.asInstanceOf[List[DynamicMap]])
  }

  def queryBuilder() = new ScalaJSQueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

  def json(oql: String, fixed: String = null, at: Any = null, parameters: Map[String, Any] = Map()): Future[String] = {
    val subst = substitute(oql, parameters)

    queryMany(subst, () => new ScalaPlainResultBuilder, fixedEntity(fixed, at)) map (r => JSON(r.arrayResult, ds.platformSpecific, 2, format = true))
  }

  private val varRegex = ":([a-zA-Z_][a-zA-Z0-9_]*)" r

  def substitute(s: String, parameters: Map[String, Any]): String = { // todo: unit tests for parameters
    if (parameters.isEmpty) s
    else
      varRegex.replaceAllIn(
        s,
        m =>
          parameters get m.group(1) match {
            case None        => sys.error(s"template: parameter '${m.group(1)}' not found")
            case Some(value) => Regex.quoteReplacement(subsrender(value))
        }
      )
  }

  def subsrender(a: Any): String =
    a match {
      case s: String =>
        s"'${s
          .replace("\\", """\\""")
          .replace("'", """\'""")
          .replace("\r", """\r""")
          .replace("\n", """\n""")}'"
      case d: js.Date           => s"'${d.toISOString()}'"
      case a: collection.Seq[_] => s"(${a map subsrender mkString ","})"
      case _                    => String.valueOf(a)
    }

  def render(a: Any, typ: Option[DataType] = None): String =
    if (typ.isDefined)
      ds.typed(a, typ.get)
    else
      a match {
        case s: String            => ds.string(s)
        case d: js.Date           => s"'${d.toISOString()}'"
        case s: collection.Seq[_] => s"(${s map (e => render(e, typ)) mkString ","})"
        case _                    => String.valueOf(a)
      }

}
