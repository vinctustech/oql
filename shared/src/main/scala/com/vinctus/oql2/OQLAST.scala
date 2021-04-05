package com.vinctus.oql2

case class OQLQuery(entity: Ident, project: List[OQLProject])

trait OQLProject { val label: Option[Ident] }
case object StarOQLProject extends OQLProject { val label: Option[Ident] = None }
case class SubtractOQLProject(id: Ident) extends OQLProject { val label: Option[Ident] = None }
case class ExpressionOQLProject(label: Option[Ident], expr: OQLExpression) extends OQLProject
case class QueryOQLProject(label: Option[Ident], query: OQLQuery) extends OQLProject

trait OQLExpression
case class InfixOQLExpression(left: OQLExpression, op: String, right: OQLExpression) extends OQLExpression
case class PrefixOQLExpression(op: String, expr: OQLExpression) extends OQLExpression
case class PostfixOQLExpression(expr: OQLExpression, op: String) extends OQLExpression
case class BetweenOQLExpression(expr: OQLExpression, op: String, lower: OQLExpression, upper: OQLExpression)
    extends OQLExpression
case class NumberOQLExpression(n: Double, pos: Position) extends OQLExpression
case class BooleanOQLExpression(b: String, pos: Position) extends OQLExpression
case class AttributeOQLExpression(ids: List[Ident]) extends OQLExpression
case class ReferenceOQLExpression(ids: List[Ident]) extends OQLExpression
case class ParameterOQLExpression(p: Ident) extends OQLExpression
case class ApplyOQLExpression(f: Ident, args: List[OQLExpression]) extends OQLExpression
case object StarOQLExpression extends OQLExpression
