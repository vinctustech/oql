package com.vinctus.oql

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExportAll
class QueryBuilder_JS_RDB private[oql] (private val oql: OQL_RDB_JS, private[oql] val q: OQLQuery, fixed: Fixed) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends QueryBuilder_JS_RDB(oql, q, fixed) {
    private def na = sys.error("not applicable")

    override def cond(v: Any): QueryBuilder_JS_RDB = na

    override def getMany(): js.Promise[js.Array[js.Any]] = na

    override def getOne(): js.Promise[js.UndefOr[Any]] = na

    override def getCount(): js.Promise[Int] = na

    override def limit(a: Int): QueryBuilder_JS_RDB = QueryBuilder_JS_RDB.this

    override def offset(a: Int): QueryBuilder_JS_RDB = QueryBuilder_JS_RDB.this

    override def order(attribute: String, sorting: String): QueryBuilder_JS_RDB = QueryBuilder_JS_RDB.this

//    override def project(source: String, attributes: String*): QueryBuilder = QueryBuilder.this
//
//    override def add(attribute: QueryBuilder): QueryBuilder = QueryBuilder.this

    override def query(query: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_RDB =
      QueryBuilder_JS_RDB.this

    override def select(s: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_RDB =
      QueryBuilder_JS_RDB.this
  }

  def cond(v: Any): QueryBuilder_JS_RDB =
    if (v != () && v != null && v != false && v != 0 && v != "") this else new DoNothingQueryBuilder

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

  def query(query: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_RDB =
    new QueryBuilder_JS_RDB(oql, oql.parseQuery(oql.substitute(query, parameters)), fixed)

  def select(s: String, parameters: js.UndefOr[js.Any] = js.undefined): QueryBuilder_JS_RDB = {
    val sel = oql.parseCondition(oql.substitute(s, parameters), q.entity)

    new QueryBuilder_JS_RDB(
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

  def order(attribute: String, sorting: String): QueryBuilder_JS_RDB = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    AbstractOQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new QueryBuilder_JS_RDB(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))), fixed)
  }

  def limit(a: Int): QueryBuilder_JS_RDB = new QueryBuilder_JS_RDB(oql, q.copy(limit = Some(a)), fixed)

  def offset(a: Int): QueryBuilder_JS_RDB = new QueryBuilder_JS_RDB(oql, q.copy(offset = Some(a)), fixed)

  def getMany(): js.Promise[js.Array[js.Any]] =
    check.oql.jsQueryMany(q, null, fixed) // empty parentheses aren't redundant: Scala.js compiler needs them

  def getOne(): js.Promise[js.UndefOr[Any]] =
    check.oql.jsQueryOne(q, null, fixed) // empty parentheses aren't redundant: Scala.js compiler needs them

  def getCount(): js.Promise[Int] =
    check.oql.count(q, null, fixed).toJSPromise // empty parentheses aren't redundant: Scala.js compiler needs them

}
