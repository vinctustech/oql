package com.vinctus.oql2

class QueryBuilder private[oql2] (private val oql: OQL, private[oql2] val q: OQLQuery) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no source was given") else this

  private class DoNothingQueryBuilder extends QueryBuilder(oql, q) {
    private def na = sys.error("not applicable")

    override def cond(b: Boolean): QueryBuilder = na

    override def getMany(parameters: Map[String, Any] = Map()): List[Any] = na

//    override def getOne: Option[Any] = na
//
//    override def getCount: Int = na

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

  def order(attribute: String, sorting: String): QueryBuilder =
    new QueryBuilder(oql, q.copy(order = Some(List(OQLOrdering(AttributeOQLExpression(List(Ident(attribute)), null), sorting)))))

  def limit(a: Int): QueryBuilder = new QueryBuilder(oql, q.copy(limit = Some(a)))

  def offset(a: Int): QueryBuilder = new QueryBuilder(oql, q.copy(offset = Some(a)))

  def getMany(parameters: Map[String, Any] = Map()): List[Any] = check.oql.queryMany(q, null, parameters)

  def getOne: Option[Any] = check.oql.queryOne(q, "", Map())

  def getCount: Int = oql.count(q, "")

//  def json: String = JSON(getMany)

}
