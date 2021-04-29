package com.vinctus.oql2

import scala.util.parsing.input.Position

trait OQLAST

case class OQLQuery(source: Ident,
                    var entity: Entity,
                    var attr: Attribute,
                    var project: List[OQLProject],
                    select: Option[OQLExpression],
                    group: Option[List[OQLExpression]],
                    order: Option[List[OQLOrdering]],
                    limit: Option[Int],
                    offset: Option[Int])
    extends OQLAST

trait OQLProject { val label: Ident }
case object StarOQLProject extends OQLProject { val label: Ident = null }
case object SQLStarOQLProject extends OQLProject { val label: Ident = null }
case class SubtractOQLProject(id: Ident) extends OQLProject { val label: Ident = null }
case class ExpressionOQLProject(label: Ident, expr: OQLExpression) extends OQLProject
case class QueryOQLProject(label: Ident, query: OQLQuery) extends OQLProject

case class OQLOrdering(expr: OQLExpression, ordering: String)
case class OQLRestrict(limit: Option[Int], offset: Option[Int])

abstract class OQLExpression { var typ: DataType = _ }
case class RawOQLExpression(s: String) extends OQLExpression
case class InArrayOQLExpression(left: OQLExpression, op: String, right: List[OQLExpression]) extends OQLExpression
case class InQueryOQLExpression(left: OQLExpression, op: String, query: OQLQuery) extends OQLExpression
case class ExistsOQLExpression(query: OQLQuery) extends OQLExpression
case class QueryOQLExpression(query: OQLQuery) extends OQLExpression
case class InfixOQLExpression(left: OQLExpression, op: String, right: OQLExpression) extends OQLExpression
case class PrefixOQLExpression(op: String, expr: OQLExpression) extends OQLExpression
case class PostfixOQLExpression(expr: OQLExpression, op: String) extends OQLExpression
case class BetweenOQLExpression(expr: OQLExpression, op: String, lower: OQLExpression, upper: OQLExpression) extends OQLExpression
case class FloatOQLExpression(n: Double) extends OQLExpression
case class IntegerOQLExpression(n: Int) extends OQLExpression
case class LiteralOQLExpression(s: String) extends OQLExpression
case class BooleanOQLExpression(b: String) extends OQLExpression
case class AttributeOQLExpression(ids: List[Ident], var dmrefs: List[(Entity, Attribute)] = null) extends OQLExpression
case class ReferenceOQLExpression(ids: List[Ident]) extends OQLExpression
case class ApplyOQLExpression(f: Ident, args: List[OQLExpression]) extends OQLExpression
case object StarOQLExpression extends OQLExpression
case class CaseOQLExpression(whens: List[OQLWhen], els: Option[OQLExpression]) extends OQLExpression
case class GroupedOQLExpression(expr: OQLExpression) extends OQLExpression

case class OQLWhen(cond: OQLExpression, expr: OQLExpression)

case class OQLKeyValuePair(key: Ident, value: OQLExpression)
case class OQLInsert(entity: Ident, values: Seq[Seq[OQLKeyValuePair]]) extends OQLAST
