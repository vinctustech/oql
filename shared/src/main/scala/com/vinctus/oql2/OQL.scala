package com.vinctus.oql2

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OQL(dm: String, val ds: SQLDataSource) {

  import OQL._

  protected var _showQuery = false

  val model: DataModel = {
    val p = new DMLParser()

    new DataModel(p.parseModel(dm), dm)
  }
//    DMLParse(dm) match {
//      case None              => sys.error("error building data model")
//      case Some(m: DMLModel) => new DataModel(m, dm)
//    }

  def connect: OQLConnection = ds.connect

  def execute[R](action: OQLConnection => Future[R]): Future[R] = {
    val conn = connect
    val res = action(conn)

    res onComplete (_ => conn.close())
    res
  }

  def create(): Future[Unit] = execute(_.create(model))

  def entity(name: String): Entity = model.entities(name)

  def parseQuery(oql: String): OQLQuery = {
    val query = OQLParser.parseQuery(oql)

    queryProjects(None, query, model, ds, oql)
    query.select foreach (decorate(query.entity, _, model, ds, oql))
    query.group foreach (_ foreach (decorate(query.entity, _, model, ds, oql)))
    query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
    query
  }

  def parseCondition(cond: String, entity: Entity): OQLExpression = {
    val expr = OQLParser.parseBooleanExpression(cond)

    decorate(entity, expr, model, ds, cond)
    expr
  }

  def count(oql: String, parameters: collection.Map[String, Any] = Map()): Future[Int] = {
    val query = OQLParser.parseQuery(oql)

    query.project = List(ExpressionOQLProject(Ident("count", null), ApplyOQLExpression(Ident("count", null), List(StarOQLExpression))))
    queryProjects(None, query, model, ds, oql)
    query.select foreach (decorate(query.entity, _, model, ds, oql))
    query.group foreach (_ foreach (decorate(query.entity, _, model, ds, oql)))
    query.copy(order = None)
    count(query, oql, parameters)
  }

  def count(query: OQLQuery, oql: String, parameters: collection.Map[String, Any]): Future[Int] =
    queryMany(query, oql, () => new ScalaResultBuilder, parameters) map { r =>
      r.arrayResult match {
        case Nil       => sys.error("count: zero rows were found")
        case List(row) => row.asInstanceOf[Map[String, Number]]("count").intValue()
        case _         => sys.error("count: more than one row was found")
      }
    }

  def queryOne(oql: String,
               newResultBuilder: () => ResultBuilder = () => new ScalaResultBuilder,
               parameters: collection.Map[String, Any] = Map()): Future[Option[Any]] = queryOne(parseQuery(oql), oql, newResultBuilder, parameters)

  def queryOne(q: OQLQuery, oql: String, newResultBuilder: () => ResultBuilder, parameters: collection.Map[String, Any]): Future[Option[Any]] =
    queryMany(q, oql, newResultBuilder, parameters) map { r =>
      r.arrayResult match {
        case Nil       => None
        case List(row) => Some(row)
        case _         => sys.error("queryOne: more than one row was found")
      }
    }

  def showQuery(): Unit = _showQuery = true

  def queryBuilder() = new QueryBuilder(this, OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None))

  def json(oql: String, parameters: collection.Map[String, Any] = Map(), tab: Int = 2, format: Boolean = true): Future[String] =
    queryMany(oql, () => new ScalaResultBuilder, parameters) map (r => JSON(r.arrayResult, ds.platformSpecific, tab, format))

  def queryMany(oql: String,
                newResultBuilder: () => ResultBuilder = () => new ScalaResultBuilder,
                parameters: collection.Map[String, Any] = Map()): Future[ResultBuilder] =
    queryMany(parseQuery(oql), oql, newResultBuilder, parameters)

  def queryMany(query: OQLQuery,
                oql: String,
                newResultBuilder: () => ResultBuilder,
                parameters: collection.Map[String, Any]): Future[ResultBuilder] = {
    val parms = new Parameters(parameters)
    val root: ResultNode = ResultNode(query, objectNode(query.project))
    val sqlBuilder = new SQLQueryBuilder(parms, oql, ds)

//    println(prettyPrint(root))
    writeQuery(root, null, Left(sqlBuilder), oql, ds)

    val sql = sqlBuilder.toString

    parameters.keySet diff parms.keySet match {
      case set: Set[_] if set.nonEmpty => sys.error(s"superfluous query parameters: ${set mkString ", "}")
      case _                           =>
    }

    if (_showQuery) {
      println(sql)
      _showQuery = false
    }

    execute { c =>
      def buildResult(node: Node, resultSet: OQLResultSet): Any =
        node match {
          case ResultNode(_, element) =>
            val result = newResultBuilder().newArray

            while (resultSet.next) result += buildResult(element, resultSet)

            result
          case n @ ManyToOneNode(_, element) =>
            if (n.idx.isDefined && resultSet.get(n.idx.get) == null) null
            else buildResult(element, resultSet)
          case n @ OneToOneNode(query, element) =>
            val sequenceResultSet = resultSet.getResultSet(n.idx)
            var rows = 0

            while (sequenceResultSet.next) rows += 1

            if (rows > 1)
              problem(query.source.pos, s"attribute '${query.source.s}' had a result set consisting of $rows rows", oql)

            if (rows == 0) null
            else buildResult(element, sequenceResultSet)
          case n @ OneToManyNode(_, element) =>
            val sequenceResultSet = resultSet.getResultSet(n.idx)
            val result = newResultBuilder().newArray

            while (sequenceResultSet.next) result += buildResult(element, sequenceResultSet)

            result.arrayResult
          case n @ ManyToManyNode(_, element) =>
            val sequenceResultSet = resultSet.getResultSet(n.idx)
            val result = newResultBuilder().newArray

            while (sequenceResultSet.next) result += buildResult(element, sequenceResultSet)

            result.arrayResult
          case v @ ValueNode(expr) =>
            val x = resultSet get v.idx

            if (v.typed) ds.convert(x, resultSet getString (v.idx + 1))
            else
              (x, expr.typ) match {
                case (s: String, IntegerType)   => s.toInt
                case (s: String, FloatType)     => s.toDouble
                case (s: String, BigintType)    => s.toLong
                case (s: String, UUIDType)      => ds.uuid(s) //UUID.fromString(s)
                case (t: String, TimestampType) => ds.timestamp(t)
//                  Instant.parse {
//                    val z =
//                      if (t.endsWith("Z")) t
//                      else if (t.endsWith("+00")) t.replace("+00", "Z")
//                      else s"${t}Z"
//
//                    if (z.charAt(10) != 'T') s"${z.substring(0, 10)}T${z.substring(11)}" else z
//                  }
                case (d: String, DecimalType(precision, scale)) => BigDecimal(d)
                case _                                          => x
              }
          case ObjectNode(properties) =>
            val result = newResultBuilder().newObject

            for ((label, node) <- properties)
              result(label) = buildResult(node, resultSet)

            result.objectResult
//          case SequenceNode(seq) => ni
        }

      c.query(sql) map { rs =>
        //      println(TextTable(rs.peer.asInstanceOf[ResultSet]))

        buildResult(root, rs).asInstanceOf[ResultBuilder]
      }
    }
  }

}

object OQL {

  private[oql2] def innerQuery(query: OQLQuery): Node =
    query.attr.typ match {
      case ManyToOneType(mtoEntity)           => ManyToOneNode(query, objectNode(query.project))
      case OneToOneType(_, _)                 => OneToOneNode(query, objectNode(query.project))
      case OneToManyType(otmEntity, attr)     => OneToManyNode(query, objectNode(query.project))
      case ManyToManyType(mtmEntity, _, _, _) => ManyToManyNode(query, objectNode(query.project))
    }

  private[oql2] def objectNode(projects: List[OQLProject]): ObjectNode = {
    ObjectNode(projects map { p =>
      (p.label.s, p match {
        case ExpressionOQLProject(label, expr) => ValueNode(expr)
        case QueryOQLProject(label, query)     => innerQuery(query)
      })
    })
  }

  private[oql2] def decorate(entity: Entity, expr: OQLExpression, model: DataModel, ds: SQLDataSource, oql: String): Unit = {
    def _decorate(expr: OQLExpression): Unit = decorate(entity, expr, model, ds, oql)

    expr match {
      case ExistsOQLExpression(query) =>
        queryProjects(Some(entity), query, model, ds, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.source.pos, s"attribute ${query.source.s} does not have an array type", oql)

        query.select foreach (decorate(query.entity, _, model, ds, oql))
        query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
      case QueryOQLExpression(query) =>
        queryProjects(Some(entity), query, model, ds, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.source.pos, s"attribute ${query.source.s} does not have an array type", oql)

        query.select foreach (decorate(query.entity, _, model, ds, oql))
        query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
      case e @ ApplyOQLExpression(f, args) =>
        args foreach _decorate

        ds.functionReturnType get f.s.toLowerCase match {
          case None =>
            val n = f.s.toLowerCase

            if ((n == "sum" || n == "avg" || n == "min" || n == "max") && args.length == 1)
              e.typ = args.head.typ

          case Some(t) => e.typ = t
        }
      case BetweenOQLExpression(expr, op, lower, upper) =>
        _decorate(expr)
        _decorate(lower)
        _decorate(upper)
      case e @ GroupedOQLExpression(expr) =>
        _decorate(expr)
        e.typ = expr.typ
      case CaseOQLExpression(whens, els) =>
        whens foreach {
          case OQLWhen(cond, expr) =>
            _decorate(cond)
            _decorate(expr)
        }

        els foreach _decorate
      case e @ PrefixOQLExpression(op, expr) =>
        _decorate(expr)
        e.typ = expr.typ
      case PostfixOQLExpression(expr, op) => _decorate(expr)
      case InArrayOQLExpression(left, op, right) =>
        _decorate(left)
        right foreach _decorate
      case InParameterOQLExpression(left, op, right) =>
        _decorate(left)
        _decorate(right)
      case e @ InfixOQLExpression(left, _, right) =>
        _decorate(left)
        _decorate(right)

        if (left.typ == right.typ)
          e.typ = left.typ
      case attrexp @ AttributeOQLExpression(ids, _) =>
        val dmrefs = new ListBuffer[(Entity, Attribute)]

        @tailrec
        def lookup(ids: List[Ident], entity: Entity): Unit =
          ids match {
            case List(id) =>
              entity.attributes get id.s match {
                case Some(attr) =>
                  dmrefs += (entity -> attr)

                  if (!attr.typ.isDataType)
                    problem(id.pos, s"attribute '${id.s}' is not a DBMS data type", oql)

                  attrexp.typ = attr.typ.asInstanceOf[DataType]
                case None => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", oql)
              }
            case head :: tail =>
              entity.attributes get head.s match {
                case Some(attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity))) =>
                  dmrefs += (mtoEntity -> attr)
                  lookup(tail, mtoEntity)
                case Some(_) => problem(head.pos, s"attribute '${head.s}' of entity '${entity.name}' does not have an entity type", oql)
                case None    => problem(head.pos, s"entity '${entity.name}' does not have attribute '${head.s}'", oql)
              }
          }

        lookup(ids, entity)
        attrexp.dmrefs = dmrefs.toList
      case InQueryOQLExpression(left, op, query) =>
        _decorate(left)
        queryProjects(Some(entity), query, model, ds, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.source.pos, s"attribute ${query.source.s} does not have an array type", oql)

        query.select foreach (decorate(query.entity, _, model, ds, oql))
        query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
      case e: LiteralOQLExpression                                                                         => e.typ = TextType
      case e: FloatOQLExpression                                                                           => e.typ = FloatType
      case e: IntegerOQLExpression                                                                         => e.typ = IntegerType
      case e: BooleanOQLExpression                                                                         => e.typ = BooleanType
      case StarOQLExpression | _: RawOQLExpression | _: ReferenceOQLExpression | _: ParameterOQLExpression =>
    }
  }

  private[oql2] def queryProjects(outer: Option[Entity], query: OQLQuery, model: DataModel, ds: SQLDataSource, oql: String): OQLQuery = {
    val map = new mutable.LinkedHashMap[String, OQLProject]
    val entity =
      if (outer.isDefined) {
        if (query.entity ne null) {
          query.entity
        } else {
          outer.get.attributes get query.source.s match {
            case Some(attr @ Attribute(name, column, pk, required, OneToOneType(entity, _))) =>
              query.entity = entity
              query.attr = attr
              entity
            case Some(attr @ Attribute(name, column, pk, required, ManyToOneType(entity))) =>
              query.entity = entity
              query.attr = attr
              entity
            case Some(attr @ Attribute(name, column, pk, required, OneToManyType(entity, otmAttr))) =>
              query.entity = entity
              query.attr = attr
              entity
            case Some(attr @ Attribute(name, column, pk, required, ManyToManyType(entity, link, self, target))) =>
              query.entity = entity
              query.attr = attr
              entity
            case None => problem(query.source.pos, s"entity '${outer.get}' does not have attribute '${query.source.s}'", oql)
          }
        }
      } else
        model.entities get query.source.s match {
          case Some(e) =>
            query.entity = e
            e
          case None => problem(query.source.pos, s"unknown entity '${query.source.s}'", oql)
        }
    val subtracts = new mutable.HashSet[String]

    query.project foreach {
      case p @ QueryOQLProject(label, query) =>
        map(label.s) = entity.attributes get query.source.s match {
          case Some(Attribute(name, column, pk, required, typ: DataType)) =>
            val attr = AttributeOQLExpression(List(query.source), null)

            decorate(entity, attr, model, ds, oql) // todo: should be done without call to 'decorate' because we have the attribute instance
            ExpressionOQLProject(label, attr)
          case Some(_) =>
            queryProjects(Some(entity), query, model, ds, oql)
            query.select foreach (decorate(query.entity, _, model, ds, oql))
            query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
            p
          case None => problem(query.source.pos, s"entity '${entity.name}' does not have attribute '${query.source.s}'", oql)
        }
      case StarOQLProject =>
        entity.attributes.values foreach {
          case attr @ Attribute(name, column, pk, required, typ) if typ.isDataType =>
            val expr = AttributeOQLExpression(List(Ident(name)), List((entity, attr)))

            expr.typ = typ.asInstanceOf[DataType]
            map(name) = ExpressionOQLProject(Ident(name), expr)
          case _ => // non-datatype attributes don't get included with '*'
        }
      case SubtractOQLProject(id) =>
        if (subtracts(id.s))
          problem(id.pos, s"attribute '${id.s}' already removed", oql)

        subtracts += id.s

        if (map contains id.s)
          map -= id.s
        else
          problem(id.pos, s"attribute '${id.s}' not added with '*'", oql)
      case expProj @ ExpressionOQLProject(label, expr) =>
        if (map contains label.s)
          problem(label.pos, s"duplicate attribute label '${label.s}'", oql)

        map(label.s) = expr match {
          case a @ AttributeOQLExpression(List(id), _) =>
            entity.attributes get id.s match {
              case Some(attr @ Attribute(_, _, _, _, _: DataType)) =>
                a.dmrefs = List((entity, attr))
                decorate(entity, a, model, ds, oql)
                expProj
              case Some(attr @ Attribute(_, _, _, _, ManyToManyType(mtmEntity, link, self, target))) =>
                QueryOQLProject(
                  label,
                  queryProjects(Some(entity), OQLQuery(id, mtmEntity, attr, List(StarOQLProject), None, None, None, None, None), model, ds, oql))
              case Some(attr @ Attribute(_, _, _, _, ManyToOneType(mtoEntity))) =>
                QueryOQLProject(
                  label,
                  queryProjects(Some(entity), OQLQuery(id, mtoEntity, attr, List(StarOQLProject), None, None, None, None, None), model, ds, oql))
              case Some(attr @ Attribute(_, _, _, _, OneToManyType(otmEntity, otmAttr))) =>
                QueryOQLProject(
                  label,
                  queryProjects(Some(entity), OQLQuery(id, otmEntity, attr, List(StarOQLProject), None, None, None, None, None), model, ds, oql))
              case None => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", oql)
            }
          case _ =>
            decorate(entity, expr, model, ds, oql)
            expProj
        }
    }

    query.project = map.values.toList
    query
  }

  private[oql2] def writeQuery(node: Node,
                               table: String,
                               builder: Either[SQLQueryBuilder, (Parameters, Int)],
                               oql: String,
                               ds: SQLDataSource): SQLQueryBuilder =
    node match {
      case ResultNode(query, element) =>
        builder.left.toOption.get.table(query.entity.table, None)
        query.select foreach (builder.left.toOption.get.select(_, query.entity.table))
        query.group foreach (builder.left.toOption.get.group(_, query.entity.table))
        query.order foreach (builder.left.toOption.get.order(_, query.entity.table))
        query.limit foreach builder.left.toOption.get.limit
        query.offset foreach builder.left.toOption.get.offset
        writeQuery(element, query.entity.table, builder, oql, ds)
        builder.left.toOption.get
      case e @ ValueNode(expr) =>
        val (idx, typed) = builder.left.toOption.get.projectValue(expr, table)

        e.idx = idx
        e.typed = typed
        builder.left.toOption.get
      case ObjectNode(properties) =>
        properties foreach { case (_, e) => writeQuery(e, table, builder, oql, ds) }
        builder.left.toOption.get
      case n @ ManyToOneNode(OQLQuery(_, entity, attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity)), _, _, _, _, _, _),
                             element) =>
        val alias = s"$table$$$name"

        if (attr.required)
          n.idx = None
        else {
          // ignore type because we only need it to check if the object is 'null'
          n.idx = Some(builder.left.toOption.get.projectValue(AttributeOQLExpression(List(Ident(name)), List((entity, attr))), table)._1) // todo: can cause 'GROUP BY' to fail in Postgres: solution is to only add this project if the attribute is not marked "non null"
        }

        builder.left.toOption.get.leftJoin(table, column, entity.table, alias, entity.pk.get.column)
        writeQuery(element, alias, builder, oql, ds)
        // todo: check query sections (i.e. order) that don't apply to many-to-one
        builder.left.toOption.get
      case n @ ManyToManyNode(OQLQuery(_,
                                       entity,
                                       Attribute(name, _, _, _, ManyToManyType(mtmEntity, linkEntity, selfAttr, targetAttr)),
                                       _,
                                       select,
                                       group,
                                       order,
                                       limit,
                                       offset),
                              element) =>
        val alias = s"$table$$$name"
        val subquery =
          if (builder.isLeft)
            new SQLQueryBuilder(builder.left.toOption.get.parms, oql, ds, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(builder.toOption.get._1, oql, ds, builder.toOption.get._2, true)
        val joinAlias = s"$alias$$${targetAttr.name}"

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(linkEntity.table, Some(alias))
        writeQuery(element, joinAlias, Left(subquery), oql, ds)
        subquery.select(RawOQLExpression(s""""$alias"."${selfAttr.column}" = "$table"."${entity.pk.get.column}""""), null)
        select foreach (subquery.select(_, joinAlias))
        group foreach (subquery.group(_, joinAlias))
        order foreach (subquery.order(_, joinAlias))
        limit foreach subquery.limit
        offset foreach subquery.offset
        subquery.innerJoin(alias, targetAttr.column, mtmEntity.table, joinAlias, mtmEntity.pk.get.column)
        subquery
      case n @ OneToOneNode(
            OQLQuery(_, entity, attr @ Attribute(name, column, pk, required, OneToOneType(mtoEntity, otmAttr)), _, select, _, order, _, _),
            element) =>
        val alias = s"$table$$$name"
        val subquery =
          if (builder.isLeft)
            new SQLQueryBuilder(builder.left.toOption.get.parms, oql, ds, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(builder.toOption.get._1, oql, ds, builder.toOption.get._2, true)

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(mtoEntity.table, Some(alias))
        writeQuery(element, alias, Left(subquery), oql, ds)
        subquery.select(RawOQLExpression(s""""$alias"."${otmAttr.column}" = "$table"."${entity.pk.get.column}""""), null)
//        select foreach (subquery.select(_, alias))  // todo: selection, ordering don't apply to one-to-one: error?
//        order foreach (subquery.ordering(_, alias))
        subquery
      case n @ OneToManyNode(OQLQuery(_,
                                      entity,
                                      attr @ Attribute(name, column, pk, required, OneToManyType(otmEntity, otmAttr)),
                                      _,
                                      select,
                                      group,
                                      order,
                                      limit,
                                      offset),
                             element) =>
        val alias = s"$table$$$name"
        val subquery =
          if (builder.isLeft)
            new SQLQueryBuilder(builder.left.toOption.get.parms, oql, ds, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(builder.toOption.get._1, oql, ds, builder.toOption.get._2, true)

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(otmEntity.table, Some(alias))
        writeQuery(element, alias, Left(subquery), oql, ds)
        subquery.select(
          RawOQLExpression(s""""$alias"."${otmAttr.column}" = "$table"."${otmAttr.typ.asInstanceOf[ManyToOneType].entity.pk.get.column}""""),
          null)
        select foreach (subquery.select(_, alias))
        group foreach (subquery.group(_, alias))
        order foreach (subquery.order(_, alias))
        limit foreach subquery.limit
        offset foreach subquery.offset
        subquery
    }

}

trait Node
case class ResultNode(query: OQLQuery, element: Node) extends Node
case class ManyToOneNode(query: OQLQuery, element: Node) extends Node { var idx: Option[Int] = _ }
case class OneToOneNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class OneToManyNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class ManyToManyNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class ObjectNode(props: Seq[(String, Node)]) extends Node // todo: objects as a way of grouping expressions
case class TupleNode(elems: Seq[Node]) extends Node // todo: tuples as a way of grouping expressions
case class ValueNode(value: OQLExpression) extends Node { var idx: Int = _; var typed: Boolean = _ }
