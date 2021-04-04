package com.vinctus.oql2

case class OQLQuery()

trait OQLExpression
case class BinaryOQLExpression(left: OQLExpression, op: String, right: OQLExpression) extends OQLExpression
case class UnaryOQLExpression(expr: OQLExpression, op: String) extends OQLExpression
case class NumberOQLExpression(n: Double, line: Int, col: Int) extends OQLExpression
case class VariableOQLExpression(v: Ident) extends OQLExpression
case class ReferenceOQLExpression(v: Ident) extends OQLExpression
case class ParameterOQLExpression(p: Ident) extends OQLExpression
case class ApplyOQLExpression(f: Ident, args: List[OQLExpression]) extends OQLExpression
case object StarOQLExpression extends OQLExpression

trait OQLProject { val label: Option[Ident] }
case class ExpressionOQLProject(label: Option[Ident], expr: OQLExpression) extends OQLProject
case class QueryOQLProject(label: Option[Ident], query: OQLQuery) extends OQLProject
