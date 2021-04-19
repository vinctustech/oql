package com.vinctus.oql2

import scala.scalajs.js.annotation.JSExport

class QueryBuilder private[oql2] (private val oql: OQL, private[oql2] val q: QueryOQL) {
  private def check = if (q.source eq null) sys.error("QueryBuilder: no resource was given") else this

  private class DoNothingQueryBuilder extends QueryBuilder(oql, q) {
    private def na = sys.error("not applicable")

    @JSExport("cond")
    override def jsCond(v: Any): QueryBuilder = na

    override def cond(b: Boolean): QueryBuilder = na

    @JSExport("getCount")
    override def jsjsGetCount(): js.Promise[Int] = na

    @JSExport("getMany")
    override def jsjsGetMany(): js.Promise[js.Array[js.Any]] = na

    @JSExport("getOne")
    override def jsjsGetOne(): js.Promise[js.Any] = na

    override def jsGetMany[T <: js.Object]: Future[T] = na

    override def jsGetOne[T <: js.Object]: Future[Option[T]] = na

    override def getMany(jsdate: Boolean = false): Future[List[DynamicMap]] = na

    override def getOne(jsdate: Boolean = false): Future[Option[DynamicMap]] = na

    override def getCount(jsdate: Boolean = false): Future[Int] = na

    @JSExport
    override def limit(a: Int): QueryBuilder = QueryBuilder.this

    @JSExport
    override def offset(a: Int): QueryBuilder = QueryBuilder.this

    @JSExport
    override def order(attribute: String, sorting: String): QueryBuilder = QueryBuilder.this

    @JSExport
    override def project(resource: String, attributes: String*): QueryBuilder = QueryBuilder.this

    @JSExport
    override def add(attribute: QueryBuilder): QueryBuilder = QueryBuilder.this

    @JSExport("query")
    override def jsQuery(q: String, parameters: js.Any = js.undefined): QueryBuilder = QueryBuilder.this

    @JSExport("select")
    override def jsSelect(s: String, parameters: js.Any = js.undefined): QueryBuilder = QueryBuilder.this

    override def query(query: String, parameters: Map[String, Any] = null): QueryBuilder = QueryBuilder.this

    override def select(s: String, parameters: Map[String, Any] = null): QueryBuilder = QueryBuilder.this
  }

  @JSExport("cond")
  def jsCond(v: Any): QueryBuilder = cond(v != () && v != null && v != false && v != 0 && v != "")

  def cond(b: Boolean): QueryBuilder = if (b) this else new DoNothingQueryBuilder

  @JSExport
  def add(attribute: QueryBuilder) =
    new QueryBuilder(
      oql,
      q.copy(project = ProjectAttributesOQL(q.project match {
        case ProjectAttributesOQL(attrs) => attrs :+ attribute.q
        case ProjectAllOQL(_)            => List(attribute.q)
      }))
    )

  @JSExport
  def add(q: String): QueryBuilder = add(query(q))

  @JSExport
  def project(resource: String, attributes: String*): QueryBuilder =
    new QueryBuilder(
      oql,
      if (attributes nonEmpty)
        q.copy(
          source = Ident(resource),
          project = ProjectAttributesOQL(attributes map {
            case "*"                     => ProjectAllOQL()
            case id if id startsWith "-" => NegativeAttribute(Ident(id drop 1))
            case a                       => QueryOQL(Ident(a), ProjectAllOQL(), None, None, None, None, None)
          })
        )
      else
        q.copy(source = Ident(resource))
    )

  @JSExport("query")
  def jsQuery(query: String, parameters: js.Any = js.undefined): QueryBuilder =
    new QueryBuilder(oql, OQLParser.parseQuery(template(query, toMap(parameters))))

  def query(query: String, parameters: Map[String, Any] = null): QueryBuilder =
    new QueryBuilder(oql, OQLParser.parseQuery(template(query, parameters)))

  @JSExport("select")
  def jsSelect(s: String, parameters: js.Any = js.undefined): QueryBuilder = select(s, toMap(parameters))

  def select(s: String, parameters: Map[String, Any] = null): QueryBuilder = {
    val sel = OQLParser.parseSelect(template(s, parameters))

    new QueryBuilder(
      oql,
      q.copy(
        select =
          if (q.select isDefined)
            Some(InfixExpressionOQL(GroupedExpressionOQL(q.select.get), "AND", GroupedExpressionOQL(sel)))
          else
            Some(sel))
    )
  }

  def order(attribute: String, sorting: String): QueryBuilder =
    new QueryBuilder(oql, q.copy(order = Some(List((VariableExpressionOQL(List(Ident(attribute))), sorting)))))

  @JSExport
  def limit(a: Int): QueryBuilder = new QueryBuilder(oql, q.copy(limit = Some(a)))

  @JSExport
  def offset(a: Int): QueryBuilder = new QueryBuilder(oql, q.copy(offset = Some(a)))

  @JSExport("getMany")
  def jsjsGetMany(): js.Promise[js.Any] = check.oql.jsjsQueryMany(q)

  @JSExport("getOne")
  def jsjsGetOne(): js.Promise[js.Any] = check.oql.jsjsQueryOne(q)

  @JSExport("getCount")
  def jsjsGetCount(): js.Promise[Int] = getCount().toJSPromise

  def jsGetMany[T <: js.Object]: Future[T] = check.oql.jsQueryMany(q)

  def jsGetOne[T <: js.Object]: Future[Option[T]] = check.oql.jsQueryOne(q)

  def getMany[T <: Product: Mappable]: Future[List[T]] = getMany() map (_ map map2cc[T])

  def getMany(jsdate: Boolean = false): Future[List[DynamicMap]] = check.oql.queryMany(q, jsdate)

  def getOne(jsdate: Boolean = false): Future[Option[DynamicMap]] = check.oql.queryOne(q, jsdate)

  def getCount(jsdate: Boolean = false): Future[Int] = oql.count(q, jsdate)

  def json: Future[String] =
    getMany().map(value => JSON.stringify(toJS(value), null.asInstanceOf[js.Array[js.Any]], 2))

}
