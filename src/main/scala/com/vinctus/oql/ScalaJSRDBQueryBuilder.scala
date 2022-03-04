package com.vinctus.oql

//import com.vinctus.mappable.{Mappable, map2cc}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

class ScalaJSRDBQueryBuilder private[oql] (
    private val oql: OQL_RDB_ScalaJS,
    private[oql] val q: OQLQuery,
    fixed: Fixed
) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends ScalaJSRDBQueryBuilder(oql, q, fixed) {
    private def na = sys.error("not applicable")

    override def cond(b: Boolean): ScalaJSRDBQueryBuilder = na

    override def getMany: Future[List[Any]] = na

//    override def ccGetMany[T <: Product: Mappable]: Future[List[T]] = na

    override def getOne: Future[Option[Any]] = na

//    override def ccGetOne[T <: Product: Mappable]: Future[Option[T]] = na

    override def jsGetMany[T <: js.Object]: Future[T] = na

    override def jsGetOne[T <: js.Object]: Future[Option[T]] = na

    override def getCount: Future[Int] = na

    override def limit(a: Int): ScalaJSRDBQueryBuilder = ScalaJSRDBQueryBuilder.this

    override def offset(a: Int): ScalaJSRDBQueryBuilder = ScalaJSRDBQueryBuilder.this

    override def order(attribute: String, sorting: String): ScalaJSRDBQueryBuilder = ScalaJSRDBQueryBuilder.this

//    override def project(source: String, attributes: String*): QueryBuilder = SJSQueryBuilder.this
//
//    override def add(attribute: QueryBuilder): QueryBuilder = SJSQueryBuilder.this

    override def query(query: String): ScalaJSRDBQueryBuilder = ScalaJSRDBQueryBuilder.this

    override def select(s: String): ScalaJSRDBQueryBuilder = ScalaJSRDBQueryBuilder.this
  }

  def cond(b: Boolean): ScalaJSRDBQueryBuilder = if (b) this else new DoNothingQueryBuilder

//  def add(attribute: QueryBuilder) =
//    new QueryBuilder(
//      oql,
//      q.copy(project = ProjectAttributesOQL(q.project match {
//        case ProjectAttributesOQL(attrs) => attrs :+ attribute.q
//        case ProjectAllOQL(_)            => List(attribute.q)
//      }))
//    )

//  def project(resource: String, attributes: String*): QueryBuilder =
//    new QueryBuilder(
//      oql,
//      if (attributes nonEmpty)
//        q.copy(
//          source = Ident(resource),
//          project = ProjectAttributesOQL(attributes map {
//            case "*"                     => ProjectAllOQL()
//            case id if id startsWith "-" => NegativeAttribute(Ident(id drop 1))
//            case a                       => QueryOQL(Ident(a), ProjectAllOQL(), None, None, None, None, None)
//          })
//        )
//      else
//        q.copy(source = Ident(resource))
//    )

  def query(query: String): ScalaJSRDBQueryBuilder = new ScalaJSRDBQueryBuilder(oql, oql.parseQuery(query), fixed)

  def select(s: String): ScalaJSRDBQueryBuilder = {
    val sel = oql.parseCondition(s, q.entity)

    new ScalaJSRDBQueryBuilder(
      oql,
      q.copy(
        select =
          if (q.select.isDefined)
            Some(InfixOQLExpression(GroupedOQLExpression(q.select.get), "AND", GroupedOQLExpression(sel)))
          else Some(sel)
      ),
      fixed
    )
  }

  def order(attribute: String, sorting: String): ScalaJSRDBQueryBuilder = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    AbstractOQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new ScalaJSRDBQueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))), fixed)
  }

  def limit(a: Int): ScalaJSRDBQueryBuilder = new ScalaJSRDBQueryBuilder(oql, q.copy(limit = Some(a)), fixed)

  def offset(a: Int): ScalaJSRDBQueryBuilder = new ScalaJSRDBQueryBuilder(oql, q.copy(offset = Some(a)), fixed)

  def jsGetMany[T <: js.Object]: Future[T] = check.oql.jsQueryMany(q)

  def jsGetOne[T <: js.Object]: Future[Option[T]] = check.oql.jsQueryOne(q, fixed)

  def getMany: Future[List[Any]] =
    check.oql.queryMany(q, null, () => new ScalaPlainResultBuilder, fixed) map (_.arrayResult.asInstanceOf[List[Any]])

//  def ccGetMany[T <: Product: Mappable]: Future[List[T]] = getMany map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def getOne: Future[Option[Any]] = check.oql.queryOne(q, "", fixed)

//  def ccGetOne[T <: Product: Mappable]: Future[Option[T]] = getOne map (_.map(m => map2cc[T](m.asInstanceOf[Map[String, Any]])))

  def getCount: Future[Int] = oql.count(q, "", fixed)

  def json: Future[String] =
    check.oql.queryMany(q, null, () => new ScalaPlainResultBuilder, fixed) map (r =>
      JSON(r.arrayResult, oql.ds.platformSpecific, format = true)
    )

}
