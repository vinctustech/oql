package com.vinctus.oql2

import com.vinctus.oql2.OQL_NodePG.jsParameters

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExport

class JSQueryBuilder private[oql2] (private val oql: OQL_NodePG, private[oql2] val q: OQLQuery) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends JSQueryBuilder(oql, q) {
    private def na = sys.error("not applicable")

    @JSExport
    override def cond(v: Any): JSQueryBuilder = na

    @JSExport
    override def getMany(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.Array[js.Any]] = na

    @JSExport
    override def getOne(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.UndefOr[Any]] = na

    @JSExport
    override def getCount(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = na

    @JSExport
    override def limit(a: Int): JSQueryBuilder = JSQueryBuilder.this

    @JSExport
    override def offset(a: Int): JSQueryBuilder = JSQueryBuilder.this

    @JSExport
    override def order(attribute: String, sorting: String): JSQueryBuilder = JSQueryBuilder.this

//    override def project(source: String, attributes: String*): QueryBuilder = QueryBuilder.this
//
//    override def add(attribute: QueryBuilder): QueryBuilder = QueryBuilder.this

    @JSExport
    override def query(query: String): JSQueryBuilder = JSQueryBuilder.this

    @JSExport
    override def select(s: String): JSQueryBuilder = JSQueryBuilder.this
  }

  @JSExport("cond")
  def cond(v: Any): JSQueryBuilder = if (v != () && v != null && v != false && v != 0 && v != "") this else new DoNothingQueryBuilder

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

  @JSExport
  def query(query: String): JSQueryBuilder = new JSQueryBuilder(oql, oql.parseQuery(query))

  @JSExport
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

  @JSExport
  def order(attribute: String, sorting: String): JSQueryBuilder = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    OQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new JSQueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))))
  }

  @JSExport
  def limit(a: Int): JSQueryBuilder = new JSQueryBuilder(oql, q.copy(limit = Some(a)))

  @JSExport
  def offset(a: Int): JSQueryBuilder = new JSQueryBuilder(oql, q.copy(offset = Some(a)))

  @JSExport
  def getMany(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.Array[js.Any]] = check.oql.jsqueryMany(q, null, parameters)

  @JSExport
  def getOne(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[js.UndefOr[Any]] = check.oql.jsqueryOne(q, null, parameters)

  @JSExport
  def getCount(parameters: js.UndefOr[js.Any] = js.undefined): js.Promise[Int] = check.oql.count(q, null, jsParameters(parameters)).toJSPromise

}
