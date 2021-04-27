package com.vinctus.oql2

import com.vinctus.oql2.OQL_TS_NodePG.jsParameters

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class JSQueryBuilder private[oql2] (private val oql: OQL_TS_NodePG, private[oql2] val q: OQLQuery) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends JSQueryBuilder(oql, q) {
    private def na = sys.error("not applicable")

    override def cond(b: Boolean): JSQueryBuilder = na

    override def getMany(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.Array[js.Any]] = na

    override def getOne(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.UndefOr[Any]] = na

    override def getCount(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = na

    override def limit(a: Int): JSQueryBuilder = JSQueryBuilder.this

    override def offset(a: Int): JSQueryBuilder = JSQueryBuilder.this

    override def order(attribute: String, sorting: String): JSQueryBuilder = JSQueryBuilder.this

//    override def project(source: String, attributes: String*): QueryBuilder = QueryBuilder.this
//
//    override def add(attribute: QueryBuilder): QueryBuilder = QueryBuilder.this

    override def query(query: String): JSQueryBuilder = JSQueryBuilder.this

    override def select(s: String): JSQueryBuilder = JSQueryBuilder.this
  }

  def cond(b: Boolean): JSQueryBuilder = if (b) this else new DoNothingQueryBuilder

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

  def query(query: String): JSQueryBuilder = new JSQueryBuilder(oql, oql.parseQuery(query))

  def select(s: String): JSQueryBuilder = {
    val sel = oql.parseCondition(s, q.entity)

    new JSQueryBuilder(
      oql,
      q.copy(
        select =
          if (q.select.isDefined) Some(InfixOQLExpression(GroupedOQLExpression(q.select.get), "AND", GroupedOQLExpression(sel)))
          else Some(sel))
    )
  }

  def order(attribute: String, sorting: String): JSQueryBuilder = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    OQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new JSQueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))))
  }

  def limit(a: Int): JSQueryBuilder = new JSQueryBuilder(oql, q.copy(limit = Some(a)))

  def offset(a: Int): JSQueryBuilder = new JSQueryBuilder(oql, q.copy(offset = Some(a)))

  def getMany(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.Array[js.Any]] = check.oql.jsqueryMany(q, null, parameters)

  def getOne(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.UndefOr[Any]] = check.oql.jsqueryOne(q, null, parameters)

  def getCount(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = check.oql.count(q, null, jsParameters(parameters)).toJSPromise

  def json(parameters: collection.Map[String, Any] = Map()): Future[String] =
    check.oql.queryMany(q, null, () => new ScalaResultBuilder, parameters) map (r => JSON(r.arrayResult, oql.ds.platformSpecific, format = true))

}
