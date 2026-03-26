package com.vinctus.oql

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

class ScalaJSPetraDBQueryBuilder private[oql] (
    private val oql: OQL_PetraDB_ScalaJS,
    private[oql] val q: OQLQuery,
    fixed: Fixed
) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends ScalaJSPetraDBQueryBuilder(oql, q, fixed) {
    private def na = sys.error("not applicable")

    override def cond(b: Boolean): ScalaJSPetraDBQueryBuilder = na

    override def getMany: Future[List[Any]] = na

    override def getOne: Future[Option[Any]] = na

    override def jsGetMany[T <: js.Object]: Future[T] = na

    override def jsGetOne[T <: js.Object]: Future[Option[T]] = na

    override def getCount: Future[Int] = na

    override def limit(a: Int): ScalaJSPetraDBQueryBuilder = ScalaJSPetraDBQueryBuilder.this

    override def offset(a: Int): ScalaJSPetraDBQueryBuilder = ScalaJSPetraDBQueryBuilder.this

    override def order(attribute: String, sorting: String): ScalaJSPetraDBQueryBuilder = ScalaJSPetraDBQueryBuilder.this

    override def query(query: String): ScalaJSPetraDBQueryBuilder = ScalaJSPetraDBQueryBuilder.this

    override def select(s: String): ScalaJSPetraDBQueryBuilder = ScalaJSPetraDBQueryBuilder.this
  }

  def cond(b: Boolean): ScalaJSPetraDBQueryBuilder = if (b) this else new DoNothingQueryBuilder

  def query(query: String): ScalaJSPetraDBQueryBuilder = new ScalaJSPetraDBQueryBuilder(oql, oql.parseQuery(query), fixed)

  def select(s: String): ScalaJSPetraDBQueryBuilder = {
    val sel = oql.parseCondition(s, q.entity)

    new ScalaJSPetraDBQueryBuilder(
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

  def order(attribute: String, sorting: String): ScalaJSPetraDBQueryBuilder = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    AbstractOQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new ScalaJSPetraDBQueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))), fixed)
  }

  def limit(a: Int): ScalaJSPetraDBQueryBuilder = new ScalaJSPetraDBQueryBuilder(oql, q.copy(limit = Some(a)), fixed)

  def offset(a: Int): ScalaJSPetraDBQueryBuilder = new ScalaJSPetraDBQueryBuilder(oql, q.copy(offset = Some(a)), fixed)

  def jsGetMany[T <: js.Object]: Future[T] = check.oql.jsQueryMany(q)

  def jsGetOne[T <: js.Object]: Future[Option[T]] = check.oql.jsQueryOne(q, fixed)

  def getMany: Future[List[Any]] =
    check.oql.queryMany(q, null, () => new ScalaPlainResultBuilder, fixed) map (_.arrayResult.asInstanceOf[List[Any]])

  def getOne: Future[Option[Any]] = check.oql.queryOne(q, "", fixed)

  def getCount: Future[Int] = oql.count(q, "", fixed)

  def json: Future[String] =
    check.oql.queryMany(q, null, () => new ScalaPlainResultBuilder, fixed) map (r =>
      JSON(r.arrayResult, oql.ds.platformSpecific, format = true)
    )

}
