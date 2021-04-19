package com.vinctus.oql2

import xyz.hyperreal.json.DefaultJSONReader

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import xyz.hyperreal._

import java.sql.ResultSet
import xyz.hyperreal.table.TextTable

import scala.annotation.tailrec
import scala.collection.immutable.VectorMap

class OQL(dm: String, val dataSource: OQLDataSource) {

  import OQL._

  val model: DataModel =
    DMLParse(dm) match {
      case None              => sys.error("error building data model")
      case Some(m: DMLModel) => new DataModel(m, dm)
    }

  def connect: OQLConnection = dataSource.connect

  def execute[R](action: OQLConnection => R): R = {
    val conn = connect
    val res = action(conn)

    conn.close()
    res
  }

  def create(): Unit = execute(_.create(model))

  def entity(name: String): Entity = model.entities(name)

  def queryMany(oql: String, parameters: Map[String, Any] = Map()) = { //todo: async
    val query =
      OQLParse(oql) match {
        case None              => sys.error("error parsing query")
        case Some(q: OQLQuery) => q
      }
    val parms = new Parameters(parameters)

    //    println(prettyPrint(query))

    queryProjects(None, query, model, oql)
    query.select foreach (attributes(query.entity, _, model, oql))

    val root: ResultNode = ResultNode(query.entity, objectNode(query.project), query.select)

//    println(prettyPrint(root))

    val sqlBuilder = new SQLQueryBuilder(parms, oql)

    writeQuery(root, null, Left(sqlBuilder), oql)

    val sql = sqlBuilder.toString

    parameters.keySet diff parms.keySet match {
      case set: Set[_] if set.nonEmpty => sys.error(s"superfluous query parameters: ${set mkString ", "}")
      case _                           =>
    }

    println(sql)

    execute { c =>
      val rs = c.query(sql)

//      println(TextTable(rs.peer.asInstanceOf[ResultSet]))

      def buildResult(node: Node, resultSet: OQLResultSet): Any =
        node match {
          case ResultNode(entity, element, select) =>
            val array = new ListBuffer[Any]

            while (resultSet.next) array += buildResult(element, resultSet)

            array.toList
          case n @ ManyToOneNode(_, element) =>
            if (resultSet.get(n.idx) == null) null
            else buildResult(element, resultSet)
          case n @ OneToOneNode(query, element) =>
            val listResultSet = new ListResultSet(DefaultJSONReader.fromString(resultSet.getString(n.idx)).asInstanceOf[List[List[Any]]])
            var rows = 0

            while (listResultSet.next) rows += 1

            if (rows > 1)
              problem(query.resource.pos, s"attribute '${query.resource.s}' had a result set consisting of $rows rows", oql)

            if (rows == 0) null
            else buildResult(element, listResultSet)
          case n @ OneToManyNode(_ibute, element) =>
            val listResultSet = new ListResultSet(DefaultJSONReader.fromString(resultSet.getString(n.idx)).asInstanceOf[List[List[Any]]])
            val array = new ListBuffer[Any]

            while (listResultSet.next) array += buildResult(element, listResultSet)

            array.toList
          case n @ ManyToManyNode(_, element) =>
            val listResultSet = new ListResultSet(DefaultJSONReader.fromString(resultSet.getString(n.idx)).asInstanceOf[List[List[Any]]])
            val array = new ListBuffer[Any]

            while (listResultSet.next) array += buildResult(element, listResultSet)

            array.toList
          case v: ValueNode => resultSet get v.idx
          case ObjectNode(properties) =>
            val map = new mutable.LinkedHashMap[String, Any]

            for ((label, node) <- properties)
              map(label) = buildResult(node, resultSet)

            map to VectorMap
//          case SequenceNode(seq) => ni
        }

      buildResult(root, rs)
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

  private[oql2] def attributes(entity: Entity, expr: OQLExpression, model: DataModel, oql: String): Unit = {
    def recur(expr: OQLExpression): Unit = attributes(entity, expr, model, oql)

    expr match {
      case ExistsOQLExpression(query) =>
        queryProjects(Some(entity), query, model, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.resource.pos, s"attribute ${query.resource.s} does not have an array type", oql)

        query.select foreach (attributes(query.entity, _, model, oql))
      case SubqueryOQLExpression(query) =>
        queryProjects(Some(entity), query, model, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.resource.pos, s"attribute ${query.resource.s} does not have an array type", oql)

        query.select foreach (attributes(query.entity, _, model, oql))
      case ApplyOQLExpression(f, args) => args foreach recur
      case BetweenOQLExpression(expr, op, lower, upper) =>
        recur(expr)
        recur(lower)
        recur(upper)
      case GroupingOQLExpression(expr) => recur(expr)
      case CaseOQLExpression(whens, els) =>
        whens foreach {
          case OQLWhen(cond, expr) =>
            recur(cond)
            recur(expr)
        }

        els foreach recur
      case PrefixOQLExpression(op, expr)  => recur(expr)
      case PostfixOQLExpression(expr, op) => recur(expr)
      case InArrayOQLExpression(left, op, right) =>
        recur(left)
        right foreach recur
      case InParameterOQLExpression(left, op, right) =>
        recur(left)
        recur(right)
      case InfixOQLExpression(left, _, right) =>
        recur(left)
        recur(right)
      case attrexp @ AttributeOQLExpression(ids, _) =>
        val dmrefs = new ListBuffer[(Entity, Attribute)]

        @tailrec
        def lookup(ids: List[Ident], entity: Entity): Unit =
          ids match {
            case List(id) =>
              entity.attributes get id.s match {
                case Some(attr) => dmrefs += (entity -> attr)
                case None       => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", oql)
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
        recur(left)
        queryProjects(Some(entity), query, model, oql)

        if (!query.attr.typ.isArrayType)
          problem(query.resource.pos, s"attribute ${query.resource.s} does not have an array type", oql)

        query.select foreach (attributes(query.entity, _, model, oql))
      case StarOQLExpression | _: RawOQLExpression | _: LiteralOQLExpression | _: FloatOQLExpression | _: IntegerOQLExpression |
          _: BooleanOQLExpression | _: ReferenceOQLExpression | _: ParameterOQLExpression =>
    }
  }

  private[oql2] def queryProjects(outer: Option[Entity], query: OQLQuery, model: DataModel, oql: String): OQLQuery = {
    val map = new mutable.LinkedHashMap[String, OQLProject]
    val entity =
      if (outer.isDefined) {
        if (query.entity ne null) {
          query.entity
        } else {
          outer.get.attributes get query.resource.s match {
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
            case None => problem(query.resource.pos, s"entity '${outer.get}' does not have attribute '${query.resource.s}'", oql)
          }
        }
      } else
        model.entities get query.resource.s match {
          case Some(e) =>
            query.entity = e
            e
          case None => problem(query.resource.pos, s"unknown entity '${query.resource.s}'", oql)
        }
    val subtracts = new mutable.HashSet[String]

    query.project foreach {
      case p @ QueryOQLProject(label, query) =>
        queryProjects(Some(entity), query, model, oql)
        query.select foreach (attributes(query.entity, _, model, oql))
        map(label.s) = p
      case StarOQLProject =>
        entity.attributes.values foreach {
          case attr @ Attribute(name, column, pk, required, typ) if typ.isDataType =>
            map(name) = ExpressionOQLProject(Ident(name), AttributeOQLExpression(List(Ident(name)), List((entity, attr))))
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
                expProj
              case Some(attr @ Attribute(_, _, _, _, ManyToManyType(mtmEntity, link, self, target))) =>
                QueryOQLProject(label,
                                queryProjects(Some(entity),
                                              OQLQuery(id, mtmEntity, attr, List(StarOQLProject), None, None, None, OQLRestrict(None, None)),
                                              model,
                                              oql))
              case Some(attr @ Attribute(_, _, _, _, ManyToOneType(mtoEntity))) =>
                QueryOQLProject(label,
                                queryProjects(Some(entity),
                                              OQLQuery(id, mtoEntity, attr, List(StarOQLProject), None, None, None, OQLRestrict(None, None)),
                                              model,
                                              oql))
              case Some(attr @ Attribute(_, _, _, _, OneToManyType(otmEntity, otmAttr))) =>
                QueryOQLProject(label,
                                queryProjects(Some(entity),
                                              OQLQuery(id, otmEntity, attr, List(StarOQLProject), None, None, None, OQLRestrict(None, None)),
                                              model,
                                              oql))
              case None => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", oql)
            }
          case _ =>
            attributes(entity, expr, model, oql)
            expProj
        }
    }

    query.project = map.values.toList
    query
  }

  private[oql2] def writeQuery(node: Node, table: String, builder: Either[SQLQueryBuilder, (Parameters, Int)], oql: String): SQLQueryBuilder =
    node match {
      case ResultNode(entity, element, select) =>
        builder.left.toOption.get.table(entity.table, None)

        if (select.isDefined)
          builder.left.toOption.get.select(select.get, entity.table)

        writeQuery(element, entity.table, builder, oql)
        builder.left.toOption.get
      case e @ ValueNode(expr) =>
        e.idx = builder.left.toOption.get.projectValue(expr, table)
        builder.left.toOption.get
      case ObjectNode(properties) =>
        properties foreach { case (_, e) => writeQuery(e, table, builder, oql) }
        builder.left.toOption.get
      case n @ ManyToOneNode(OQLQuery(_, entity, attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity)), _, _, _, _, _), element) =>
        val alias = s"$table$$$name"

        n.idx = builder.left.toOption.get.projectValue(AttributeOQLExpression(List(Ident(name)), List((entity, attr))), table)
        builder.left.toOption.get.leftJoin(table, column, entity.table, alias, entity.pk.get.column)
        writeQuery(element, alias, builder, oql)
        builder.left.toOption.get
      case n @ ManyToManyNode(
            OQLQuery(_, entity, attr @ Attribute(name, _, _, _, ManyToManyType(mtmEntity, linkEntity, selfAttr, targetAttr)), _, select, _, _, _),
            element) =>
        val alias = s"$table$$$name"
        val subquery =
          if (builder.isLeft) new SQLQueryBuilder(builder.left.toOption.get.parms, oql, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(builder.toOption.get._1, oql, builder.toOption.get._2, true)
        val joinAlias = s"$alias$$${targetAttr.name}"

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(linkEntity.table, Some(alias))
        writeQuery(element, joinAlias, Left(subquery), oql)
        subquery.select(RawOQLExpression(s"$alias.${selfAttr.column} = $table.${entity.pk.get.column}"), null)
        select foreach (subquery.select(_, joinAlias))
        subquery.innerJoin(alias, targetAttr.column, mtmEntity.table, joinAlias, mtmEntity.pk.get.column)
        subquery
      case n @ OneToOneNode(OQLQuery(_, entity, attr @ Attribute(name, column, pk, required, OneToOneType(mtoEntity, otmAttr)), _, select, _, _, _),
                            element) =>
        val alias = s"$table$$$name"
        val subquery =
          if (builder.isLeft) new SQLQueryBuilder(builder.left.toOption.get.parms, oql, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(builder.toOption.get._1, oql, builder.toOption.get._2, true)

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(mtoEntity.table, Some(alias))
        writeQuery(element, alias, Left(subquery), oql)
        subquery.select(RawOQLExpression(s"$alias.${otmAttr.column} = $table.${entity.pk.get.column}"), null)
        select foreach (subquery.select(_, alias))
        subquery
      case n @ OneToManyNode(OQLQuery(_, entity, attr @ Attribute(name, column, pk, required, OneToManyType(mtoEntity, otmAttr)), _, select, _, _, _),
                             element) =>
        val alias = s"$table$$$name"
        val subquery =
          if (builder.isLeft) new SQLQueryBuilder(builder.left.toOption.get.parms, oql, builder.left.toOption.get.margin + 2 * SQLQueryBuilder.INDENT)
          else new SQLQueryBuilder(builder.toOption.get._1, oql, builder.toOption.get._2, true)

        if (builder.isLeft)
          n.idx = builder.left.toOption.get.projectQuery(subquery)

        subquery.table(mtoEntity.table, Some(alias))
        writeQuery(element, alias, Left(subquery), oql)
        subquery.select(RawOQLExpression(s"$alias.${otmAttr.column} = $table.${entity.pk.get.column}"), null)
        select foreach (subquery.select(_, alias))
        subquery
    }

}

trait Node
case class ResultNode(entity: Entity, element: Node, select: Option[OQLExpression]) extends Node
case class ManyToOneNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class OneToOneNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class OneToManyNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class ManyToManyNode(query: OQLQuery, element: Node) extends Node { var idx: Int = _ }
case class ObjectNode(props: Seq[(String, Node)]) extends Node // todo: objects as a way of grouping expressions
case class TupleNode(elems: Seq[Node]) extends Node // todo: tuples as a way of grouping expressions
case class ValueNode(value: OQLExpression) extends Node { var idx: Int = _ }
