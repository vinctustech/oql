package com.vinctus.oql2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QueryBuilder private[oql2] (private val oql: OQL, private[oql2] val q: OQLQuery) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends QueryBuilder(oql, q) {
    private def na = sys.error("not applicable")

    override def cond(b: Boolean): QueryBuilder = na

    override def getMany(newResultBuilder: () => ResultBuilder = () => new ScalaResultBuilder,
                         parameters: Map[String, Any] = Map()): Future[ResultBuilder] = na

    override def getOne(newResultBuilder: () => ResultBuilder = () => new ScalaResultBuilder,
                        parameters: Map[String, Any] = Map()): Future[Option[Any]] = na

    override def getCount: Future[Int] = na

    override def limit(a: Int): QueryBuilder = QueryBuilder.this

    override def offset(a: Int): QueryBuilder = QueryBuilder.this

    override def order(attribute: String, sorting: String): QueryBuilder = QueryBuilder.this

//    override def project(source: String, attributes: String*): QueryBuilder = QueryBuilder.this
//
//    override def add(attribute: QueryBuilder): QueryBuilder = QueryBuilder.this

    override def query(query: String): QueryBuilder = QueryBuilder.this

    override def select(s: String): QueryBuilder = QueryBuilder.this
  }

  def cond(b: Boolean): QueryBuilder = if (b) this else new DoNothingQueryBuilder

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

  def query(query: String): QueryBuilder =
    new QueryBuilder(oql, oql.parseQuery(query))

  def select(s: String): QueryBuilder = {
    val sel = oql.parseCondition(s, q.entity)

    new QueryBuilder(
      oql,
      q.copy(
        select =
          if (q.select.isDefined) Some(InfixOQLExpression(GroupedOQLExpression(q.select.get), "AND", GroupedOQLExpression(sel)))
          else Some(sel))
    )
  }

  def order(attribute: String, sorting: String): QueryBuilder = {
    val attr = AttributeOQLExpression(List(Ident(attribute)), null)

    OQL.decorate(q.entity, attr, oql.model, oql.ds, null)

    new QueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(attr, sorting)))))
  }

  def limit(a: Int): QueryBuilder = new QueryBuilder(oql, q.copy(limit = Some(a)))

  def offset(a: Int): QueryBuilder = new QueryBuilder(oql, q.copy(offset = Some(a)))

  def getMany(newResultBuilder: () => ResultBuilder = () => new ScalaResultBuilder, parameters: Map[String, Any] = Map()): Future[ResultBuilder] =
    check.oql.queryMany(q, null, newResultBuilder, parameters)

  def getOne(newResultBuilder: () => ResultBuilder = () => new ScalaResultBuilder, parameters: Map[String, Any] = Map()): Future[Option[Any]] =
    check.oql.queryOne(q, "", newResultBuilder, parameters)

  def getCount: Future[Int] = oql.count(q, "")

  def json(parameters: Map[String, Any] = Map(), tab: Int = 2, format: Boolean = true): Future[String] =
    getMany(() => new ScalaResultBuilder, parameters) map (r => JSON(r.arrayResult, oql.ds.platformSpecific, format = true))

}
