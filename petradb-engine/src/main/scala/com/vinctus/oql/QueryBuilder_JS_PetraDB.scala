package com.vinctus.oql

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExportAll
class QueryBuilder_JS_PetraDB private[oql] (private val oql: OQL_PetraDB_JS, private[oql] val q: OQLQuery, fixed: Fixed) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends QueryBuilder_JS_PetraDB(oql, q, fixed) {
    private def na = sys.error("not applicable")

    override def cond(v: Any): QueryBuilder_JS_PetraDB = na

    override def getMany(): js.Promise[js.Array[js.Any]] = na

    override def getOne(): js.Promise[js.UndefOr[Any]] = na

    override def getCount(): js.Promise[Int] = na

    override def limit(a: Int): QueryBuilder_JS_PetraDB = QueryBuilder_JS_PetraDB.this

    override def offset(a: Int): QueryBuilder_JS_PetraDB = QueryBuilder_JS_PetraDB.this

    override def order(attribute: String, sorting: String): QueryBuilder_JS_PetraDB = QueryBuilder_JS_PetraDB.this

    override def query(query: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_PetraDB =
      QueryBuilder_JS_PetraDB.this

    override def select(s: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_PetraDB =
      QueryBuilder_JS_PetraDB.this
  }

  def cond(v: Any): QueryBuilder_JS_PetraDB =
    if (v != () && v != null && v != false && v != 0 && v != "") this else new DoNothingQueryBuilder

  def query(query: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_PetraDB =
    new QueryBuilder_JS_PetraDB(oql, oql.parseQuery(oql.substitute(query, parameters)), fixed)

  def select(s: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_PetraDB = {
    val sel = oql.parseCondition(oql.substitute(s, parameters), q.entity)

    new QueryBuilder_JS_PetraDB(
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

  def order(attribute: String, sorting: String): QueryBuilder_JS_PetraDB = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    AbstractOQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new QueryBuilder_JS_PetraDB(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))), fixed)
  }

  def limit(a: Int): QueryBuilder_JS_PetraDB = new QueryBuilder_JS_PetraDB(oql, q.copy(limit = Some(a)), fixed)

  def offset(a: Int): QueryBuilder_JS_PetraDB = new QueryBuilder_JS_PetraDB(oql, q.copy(offset = Some(a)), fixed)

  def getMany(): js.Promise[js.Array[js.Any]] =
    check.oql.jsQueryMany(q, null, fixed)

  def getOne(): js.Promise[js.UndefOr[Any]] =
    check.oql.jsQueryOne(q, null, fixed)

  def getCount(): js.Promise[Int] =
    check.oql.count(q, null, fixed).toJSPromise

}
