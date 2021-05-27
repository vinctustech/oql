package com.vinctus.oql2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NodePGQueryBuilder private[oql2] (private val oql: OQL_NodePG, private[oql2] val q: OQLQuery) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingNodePGQueryBuilder extends NodePGQueryBuilder(oql, q) {
    private def na = sys.error("not applicable")

    override def cond(b: Boolean): NodePGQueryBuilder = na

    override def getMany: Future[ResultBuilder] = na

    override def getOne: Future[Option[Any]] = na

    override def getCount: Future[Int] = na

    override def limit(a: Int): NodePGQueryBuilder = NodePGQueryBuilder.this

    override def offset(a: Int): NodePGQueryBuilder = NodePGQueryBuilder.this

    override def order(attribute: String, sorting: String): NodePGQueryBuilder = NodePGQueryBuilder.this

//    override def project(source: String, attributes: String*): QueryBuilder = QueryBuilder.this
//
//    override def add(attribute: QueryBuilder): QueryBuilder = QueryBuilder.this

    override def query(query: String): NodePGQueryBuilder = NodePGQueryBuilder.this

    override def select(s: String): NodePGQueryBuilder = NodePGQueryBuilder.this
  }

  def cond(b: Boolean): NodePGQueryBuilder = if (b) this else new DoNothingNodePGQueryBuilder

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

  def query(query: String): NodePGQueryBuilder = new NodePGQueryBuilder(oql, oql.parseQuery(query))

  def select(s: String): NodePGQueryBuilder = {
    val sel = oql.parseCondition(s, q.entity)

    new NodePGQueryBuilder(
      oql,
      q.copy(
        select =
          if (q.select.isDefined) Some(InfixOQLExpression(GroupedOQLExpression(q.select.get), "AND", GroupedOQLExpression(sel)))
          else Some(sel))
    )
  }

  def order(attribute: String, sorting: String): NodePGQueryBuilder = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    AbstractOQL.decorate(q.entity, attr, oql.model, oql.ds, null)
    new NodePGQueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))))
  }

  def limit(a: Int): NodePGQueryBuilder = new NodePGQueryBuilder(oql, q.copy(limit = Some(a)))

  def offset(a: Int): NodePGQueryBuilder = new NodePGQueryBuilder(oql, q.copy(offset = Some(a)))

  def getMany: Future[ResultBuilder] =
    check.oql.queryMany(q, null, () => new ScalaResultBuilder)

  def getOne: Future[Option[Any]] =
    check.oql.queryOne(q, "")

  def getCount: Future[Int] = oql.count(q, "")

  def json: Future[String] =
    check.oql.queryMany(q, null, () => new ScalaResultBuilder) map (r => JSON(r.arrayResult, oql.ds.platformSpecific, format = true))

}