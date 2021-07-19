package com.vinctus.oql

import com.vinctus.sjs_utils.DynamicMap

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class AbstractOQL(dm: String, val ds: SQLDataSource, conv: Conversions) {

  import AbstractOQL._

  private var _transpileOnly = false
  private var _showQuery = false

  val model: DataModel = new DataModel(new DMLParser().parseModel(dm), dm)
//    DMLParse(dm) match {
//      case None              => sys.error("error building data model")
//      case Some(m: DMLModel) => new DataModel(m, dm)
//    }

  def connect: OQLConnection = ds.connect

  def render(a: Any, typ: Option[DataType] = None): String

  def execute[R](action: OQLConnection => Future[R]): Future[R]
//    val conn = connect
//    val res = action(conn)
//
//    res onComplete (_ => conn.close())
//    res
//  }

  def create(): Future[Unit] = execute(_.create(model))

  def parseQuery(oql: String): OQLQuery = {
    val query = OQLParser.parseQuery(oql)

    preprocessQuery(None, query, model, ds, oql) // todo: should be called "preprocessQuery" and do all decorating
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

  def count(oql: String): Future[Int] = count(OQLParser.parseQuery(oql), oql)

  def count(query: OQLQuery, oql: String): Future[Int] = {
    query.project = List(ExpressionOQLProject(Ident("count", null), ApplyOQLExpression(Ident("count", null), List(StarOQLExpression))))
    preprocessQuery(None, query, model, ds, oql)
    query.select foreach (decorate(query.entity, _, model, ds, oql))
    query.group foreach (_ foreach (decorate(query.entity, _, model, ds, oql)))

    queryMany(query.copy(order = None), oql, () => new ScalaResultBuilder, null) map {
      _.arrayResult match {
        case Nil       => sys.error("count: zero rows were found")
        case List(row) => row.asInstanceOf[Map[String, Number]]("count").intValue()
        case a         => sys.error(s"count: more than one row was found: $a")
      }
    }
  }

  def showQuery(): Unit = _showQuery = true

  def transpileOnly(): Unit = _transpileOnly = true

  private[oql] def show(sql: String): Unit = {
    if (_showQuery || _transpileOnly) {
      println(sql)
      _showQuery = false
    }
  }

  private[oql] def exec: Boolean = !_transpileOnly

  def queryOne(q: OQLQuery, oql: String): Future[Option[DynamicMap]] =
    queryMany(q, oql, () => new SJSResultBuilder, null) map {
      _.arrayResult match {
        case Nil       => None
        case List(row) => Some(row.asInstanceOf[DynamicMap])
        case _         => sys.error(s"queryOne: more than one row was found")
      }
    }

  protected def fixedEntity(entity: String, value: Any): Fixed =
    if (entity ne null) Fixed(operative = true, model.entities(entity), value)
    else Fixed(operative = false)

  def queryMany(oql: String, newResultBuilder: () => ResultBuilder, fixed: Fixed): Future[ResultBuilder] =
    queryMany(parseQuery(oql), oql, newResultBuilder, fixed)

  def queryMany(query: OQLQuery, oql: String, newResultBuilder: () => ResultBuilder, fixed: Fixed): Future[ResultBuilder] = {
    val root: ResultNode = ResultNode(query, objectNode(query.project))
    val sqlBuilder = new SQLQueryBuilder(oql, ds, fixed, model)

//    println(prettyPrint(root))
    writeQuery(root, null, Left(sqlBuilder), oql, ds, fixed, model)

    val sql = sqlBuilder.toString

    show(sql)

    if (exec) {
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
            case n @ ValueNode(expr) =>
              val v = resultSet get n.idx
              val typ =
                if (n.typed) ds.reverseMapType(resultSet getString (n.idx + 1))
                else expr.typ

              (v, typ) match {
                case (s: String, IntegerType)   => s.toInt
                case (s: String, FloatType)     => s.toDouble
                case (s: String, BigintType)    => conv.long(s)
                case (s: String, UUIDType)      => conv.uuid(s)
                case (t: String, TimestampType) => conv.timestamp(t)
//                  Instant.parse {
//                    val z =
//                      if (t.endsWith("Z")) t
//                      else if (t.endsWith("+00")) t.replace("+00", "Z")
//                      else s"${t}Z"
//
//                    if (z.charAt(10) != 'T') s"${z.substring(0, 10)}T${z.substring(11)}" else z
//                  }
                case (d: String, DecimalType(precision, scale)) => conv.decimal(d, precision, scale)
                case _                                          => v
              }
            case ObjectNode(properties) =>
              val result = newResultBuilder().newObject

              for ((label, node) <- properties)
                result(label) = buildResult(node, resultSet)

              result.objectResult
//          case SequenceNode(seq) => ni
          }

        c.command(sql) map { rs =>
          //      println(TextTable(rs.peer.asInstanceOf[ResultSet]))
          buildResult(root, rs).asInstanceOf[ResultBuilder]
        }
      }
    } else Future(null)
  }

}

object AbstractOQL {

  private[oql] def innerQuery(query: OQLQuery): Node =
    query.attr.typ match {
      case _: ManyToOneType  => ManyToOneNode(query, objectNode(query.project))
      case _: OneToOneType   => OneToOneNode(query, objectNode(query.project))
      case _: OneToManyType  => OneToManyNode(query, objectNode(query.project))
      case _: ManyToManyType => ManyToManyNode(query, objectNode(query.project))
    }

  private[oql] def objectNode(projects: List[OQLProject]): ObjectNode = {
    ObjectNode(projects map { p =>
      (p.label.s, p match {
        case ExpressionOQLProject(_, expr) => ValueNode(expr)
        case QueryOQLProject(_, query)     => innerQuery(query)
      })
    })
  }

  private[oql] def decorate(entity: Entity, expr: OQLExpression, model: DataModel, ds: SQLDataSource, oql: String): Unit = {
    def _decorate(expr: OQLExpression): Unit = decorate(entity, expr, model, ds, oql)

//    def lookup(expr: OQLExpression, ids: List[Ident], ref: Boolean): List[(Entity, Attribute)] = {
//      val dmrefs = new ListBuffer[(Entity, Attribute)]
//
//      @tailrec
//      def lookup(ids: List[Ident], entity: Entity): Unit =
//        ids match {
//          case List(id) =>
//            entity.attributes get id.s match {
//              case Some(attr) =>
//                dmrefs += (entity -> attr)
//
//                if (ref) {
//                  if (!attr.typ.isInstanceOf[ManyToOneType])
//                    problem(id.pos, s"attribute '${id.s}' is not many-to-one", oql)
//
//                  expr.typ = attr.typ.asInstanceOf[ManyToOneType].entity.pk.get.typ.asInstanceOf[DataType]
//                } else {
//                  if (!attr.typ.isDataType)
//                    problem(id.pos, s"attribute '${id.s}' is not a DBMS data type", oql)
//
//                  expr.typ = attr.typ.asInstanceOf[DataType]
//                }
//              case None => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", oql)
//            }
//          case head :: tail =>
//            entity.attributes get head.s match {
//              case Some(attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity))) =>
//                dmrefs += (mtoEntity -> attr)
//                lookup(tail, mtoEntity)
//              case Some(_) => problem(head.pos, s"attribute '${head.s}' of entity '${entity.name}' does not have an entity type", oql)
//              case None    => problem(head.pos, s"entity '${entity.name}' does not have attribute '${head.s}'", oql)
//            }
//        }
//
//      lookup(ids, entity)
//      dmrefs.toList
//    }

    expr match {
      case ExistsOQLExpression(query) =>
        query.project = List(SQLStarOQLProject)
        preprocessQuery(Some(entity), query, model, ds, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.source.pos, s"attribute ${query.source.s} does not have an array type", oql)

        query.select foreach (decorate(query.entity, _, model, ds, oql))
        query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
      case QueryOQLExpression(query) =>
        preprocessQuery(Some(entity), query, model, ds, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.source.pos, s"attribute ${query.source.s} does not have an array type", oql)

        query.select foreach (decorate(query.entity, _, model, ds, oql))
        query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
      case e @ ApplyOQLExpression(f, args) =>
        args foreach _decorate

        ds.functionReturnType get (f.s.toLowerCase, args.length) match {
          case None =>
            val n = f.s.toLowerCase

            if ((n == "sum" || n == "avg" || n == "min" || n == "max") && args.length == 1)
              e.typ = args.head.typ

          case Some(t) => e.typ = t(args map (_.typ))
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
      case e @ InfixOQLExpression(left, _, right) =>
        _decorate(left)
        _decorate(right)

        if (left.typ == right.typ)
          e.typ = left.typ
      case attrexp @ ReferenceOQLExpression(ids, _) => attrexp.dmrefs = model.lookup(attrexp, ids, ref = true, entity, oql)
      case AttributeOQLExpression(List(id), _) if ds.builtinVariables contains id.s =>
        expr.typ = ds.builtinVariables(id.s) // it's a built-in variable so assign type from `builtinVariables` map but leave dmrefs null
      case attrexp @ AttributeOQLExpression(ids, _) => attrexp.dmrefs = model.lookup(attrexp, ids, ref = false, entity, oql)
      case InQueryOQLExpression(left, op, query) =>
        _decorate(left)
        preprocessQuery(Some(entity), query, model, ds, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.source.pos, s"attribute ${query.source.s} does not have an array type", oql)

        query.select foreach (decorate(query.entity, _, model, ds, oql))
        query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
      case e: StringOQLExpression                  => e.typ = TextType
      case e: FloatOQLExpression                   => e.typ = FloatType
      case e: IntegerOQLExpression                 => e.typ = IntegerType
      case e: BooleanOQLExpression                 => e.typ = BooleanType
      case e @ TypedOQLExpression(_, t)            => e.typ = t
      case StarOQLExpression | _: RawOQLExpression =>
    }
  }

  private[oql] def preprocessQuery(outer: Option[Entity], query: OQLQuery, model: DataModel, ds: SQLDataSource, oql: String): OQLQuery = {
    val map = new mutable.LinkedHashMap[String, OQLProject]
    val entity =
      if (outer.isDefined) {
        if (query.entity ne null) {
          query.entity
        } else {
          outer.get.attributes get query.source.s match {
            case Some(attr @ Attribute(name, column, pk, required, typ: RelationalType)) =>
              query.entity = typ.entity
              query.attr = attr
              typ.entity
            case None => problem(query.source.pos, s"entity '${outer.get}' does not have relational attribute '${query.source.s}'", oql)
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
      case proj @ QueryOQLProject(label, query) =>
        map(label.s) = entity.attributes get query.source.s match {
          case Some(Attribute(_, _, _, _, _: DataType)) => // an attribute with a DataType
            val attr = AttributeOQLExpression(List(query.source), null)

            decorate(entity, attr, model, ds, oql) // todo: should be done without call to 'decorate' because we have the attribute instance
            ExpressionOQLProject(label, attr)
          case Some(_) =>
            preprocessQuery(Some(entity), query, model, ds, oql)
            query.select foreach (decorate(query.entity, _, model, ds, oql))
            query.order foreach (_ foreach { case OQLOrdering(expr, _) => decorate(query.entity, expr, model, ds, oql) })
            proj
          case None if ds.builtinVariables contains query.source.s =>
            ExpressionOQLProject(label, RawOQLExpression(query.source.s)) // it's a built-in variable so sent it through raw
          case None => problem(query.source.pos, s"entity '${entity.name}' does not have attribute '${query.source.s}'", oql)
        }
      case StarOQLProject =>
        entity.attributes.values foreach {
          case attr @ Attribute(name, column, pk, required, typ) if typ.isDataType =>
            val expr = AttributeOQLExpression(List(Ident(name)), List((entity, attr)))

            expr.typ = typ.asDatatype
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
                decorate(entity, a, model, ds, oql) // todo: shouldn't have to call `decorate` because we have the Attribute object
                expProj
              case Some(attr @ Attribute(_, _, _, _, ManyToManyType(mtmEntity, link, self, target))) =>
                QueryOQLProject(
                  label,
                  preprocessQuery(Some(entity), OQLQuery(id, mtmEntity, attr, List(StarOQLProject), None, None, None, None, None), model, ds, oql))
              case Some(attr @ Attribute(_, _, _, _, ManyToOneType(mtoEntity))) =>
                QueryOQLProject(
                  label,
                  preprocessQuery(Some(entity), OQLQuery(id, mtoEntity, attr, List(StarOQLProject), None, None, None, None, None), model, ds, oql))
              case Some(attr @ Attribute(_, _, _, _, OneToManyType(otmEntity, otmAttr))) =>
                QueryOQLProject(
                  label,
                  preprocessQuery(Some(entity), OQLQuery(id, otmEntity, attr, List(StarOQLProject), None, None, None, None, None), model, ds, oql))
              case None if ds.builtinVariables contains id.s => expProj // it's a built-in variable so leave dmrefs null and let it go through
              case None                                      => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", oql)
            }
          case _ =>
            decorate(entity, expr, model, ds, oql)
            expProj
        }
      case SQLStarOQLProject => map(null) = ExpressionOQLProject(Ident(null), StarOQLExpression)
    }

    query.project = map.values.toList
    query
  }

  private[oql] def writeQuery(node: Node,
                              table: String,
                              builder: Either[SQLQueryBuilder, Int],
                              oql: String,
                              ds: SQLDataSource,
                              fixed: Fixed,
                              model: DataModel): SQLQueryBuilder =
    node match {
      case ResultNode(query, element) =>
        builder.left.toOption.get.table(query.entity.table, None)
        query.select foreach (builder.left.toOption.get.select(_, query.entity.table))

        // generate conditions for fixed entity if necessary
        if (fixed.operative) {
          for (attr <- query.entity.fixing(fixed.entity)) {
            builder.left.toOption.get
              .select(InfixOQLExpression(attr, "=", TypedOQLExpression(fixed.at, fixed.entity.pk.get.typ.asDatatype)), query.entity.table)
          }
        }

        query.group foreach (builder.left.toOption.get.group(_, query.entity.table))
        query.order foreach (builder.left.toOption.get.order(_, query.entity.table))
        query.limit foreach builder.left.toOption.get.limit
        query.offset foreach builder.left.toOption.get.offset
        writeQuery(element, query.entity.table, builder, oql, ds, fixed, model)
        builder.left.toOption.get
      case e @ ValueNode(expr) =>
        val (idx, typed) = builder.left.toOption.get.projectValue(expr, table)

        e.idx = idx
        e.typed = typed
        builder.left.toOption.get
      case ObjectNode(properties) =>
        properties foreach { case (_, e) => writeQuery(e, table, builder, oql, ds, fixed, model) }
        builder.left.toOption.get
      case n @ ManyToOneNode(OQLQuery(_, entity, attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity)), _, _, _, _, _, _),
                             element) =>
        val alias = s"$table$$$name"

        if (attr.required)
          n.idx = None
        else {
          val mtoAttr = AttributeOQLExpression(List(Ident(name)), List((entity, attr)))

          mtoAttr.typ = mtoEntity.pk.get.typ.asDatatype // add type because we don't want SQLQueryBuilder to generate "typeof" function call
          n.idx = Some(builder.left.toOption.get.projectValue(mtoAttr, table)._1)
        }

        builder.left.toOption.get.leftJoin(table, column, entity.table, alias, entity.pk.get.column)
        writeQuery(element, alias, builder, oql, ds, fixed, model)
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
            new SQLQueryBuilder(oql, ds, fixed, model, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(oql, ds, fixed, model, builder.toOption.get, true)
        val joinAlias = s"$alias$$${targetAttr.name}"

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(linkEntity.table, Some(alias))
        writeQuery(element, joinAlias, Left(subquery), oql, ds, fixed, model)
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
            new SQLQueryBuilder(oql, ds, fixed, model, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(oql, ds, fixed, model, builder.toOption.get, true)

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(mtoEntity.table, Some(alias))
        writeQuery(element, alias, Left(subquery), oql, ds, fixed, model)
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
            new SQLQueryBuilder(oql, ds, fixed, model, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(oql, ds, fixed, model, builder.toOption.get, true)

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(otmEntity.table, Some(alias))
        writeQuery(element, alias, Left(subquery), oql, ds, fixed, model)
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

  trait Node
  case class ResultNode(query: OQLQuery, element: Node) extends Node
  case class ManyToOneNode(query: OQLQuery, element: Node) extends Node { var idx: Option[Int] = _ }
  case class OneToOneNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
  case class OneToManyNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
  case class ManyToManyNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
  case class ObjectNode(props: Seq[(String, Node)]) extends Node // todo: objects as a way of grouping expressions
  case class TupleNode(elems: Seq[Node]) extends Node // todo: tuples as a way of grouping expressions
  case class ValueNode(value: OQLExpression) extends Node { var idx: Int = _; var typed: Boolean = _ }

}

case class Fixed(operative: Boolean, entity: Entity = null, at: Any = null)
