package com.vinctus.oql

import scala.scalajs.js

// Builds a raw (undecorated) OQLQuery from a plain JS object tree — the same
// shape OQLParser produces, so the result feeds the existing
// processQuery -> decorate -> queryMany pipeline unchanged. Lets oql-typed
// hand over a pre-built AST and skip the string parser entirely.
//
// The wire format uses `kind` as the discriminator on every node (see the
// node contract in oql-typed's buildAST). Field names are read off js.Dynamic
// via selectDynamic, e.g. `o.source`, `o.left`.
object FromJS {

  private def kind(o: js.Dynamic): String = o.kind.asInstanceOf[String]

  private def opt(v: js.Any): Option[js.Dynamic] =
    if (js.isUndefined(v) || v == null) None else Some(v.asInstanceOf[js.Dynamic])

  private def arr(v: js.Any): List[js.Dynamic] =
    if (js.isUndefined(v) || v == null) Nil
    else v.asInstanceOf[js.Array[js.Dynamic]].toList

  private def idents(o: js.Dynamic): List[Ident] =
    o.ids.asInstanceOf[js.Array[String]].toList map (Ident(_))

  def fromJS(o: js.Dynamic): OQLQuery = toQuery(o)

  private def projects(o: js.Dynamic): List[OQLProject] =
    opt(o.project) match {
      case Some(p) => arr(p) map toProject
      case None    => List(StarOQLProject)
    }

  private def toQuery(o: js.Dynamic): OQLQuery =
    OQLQuery(
      Ident(o.source.asInstanceOf[String]),
      null,
      null,
      projects(o),
      opt(o.select) map toExpr,
      None,
      opt(o.order) map (os => arr(os) map toOrdering),
      opt(o.limit) map (_.asInstanceOf[Int]),
      opt(o.offset) map (_.asInstanceOf[Int]),
    )

  private def toProject(o: js.Dynamic): OQLProject =
    kind(o) match {
      case "field" =>
        val name = o.name.asInstanceOf[String]
        ExpressionOQLProject(Ident(name), AttributeOQLExpression(List(Ident(name))))
      case "expr" =>
        ExpressionOQLProject(Ident(o.label.asInstanceOf[String]), toExpr(o.expr))
      case "rel" =>
        QueryOQLProject(
          Ident(o.label.asInstanceOf[String]),
          OQLQuery(
            Ident(o.source.asInstanceOf[String]),
            null,
            null,
            projects(o),
            opt(o.select) map toExpr,
            None,
            opt(o.order) map (os => arr(os) map toOrdering),
            None,
            None,
          ),
        )
      case other => sys.error(s"fromJS: unknown projection kind '$other'")
    }

  // Mirror OQLParser's ASC/DESC -> NULLS normalization so the AST path emits
  // the same ordering clause the string path would (OQLParser ordering rule).
  private def toOrdering(o: js.Dynamic): OQLOrdering = {
    val ordering = o.dir.asInstanceOf[String].toUpperCase match {
      case "ASC"  => "ASC NULLS FIRST"
      case "DESC" => "DESC NULLS LAST"
      case other  => other
    }
    OQLOrdering(toExpr(o.expr), ordering)
  }

  private def toExpr(o: js.Dynamic): OQLExpression =
    kind(o) match {
      case "attr" => AttributeOQLExpression(idents(o))
      case "ref"  => ReferenceOQLExpression(idents(o))
      case "infix" =>
        InfixOQLExpression(toExpr(o.left), o.op.asInstanceOf[String], toExpr(o.right))
      case "prefix"  => PrefixOQLExpression(o.op.asInstanceOf[String], toExpr(o.expr))
      case "postfix" => PostfixOQLExpression(toExpr(o.expr), o.op.asInstanceOf[String])
      case "between" =>
        BetweenOQLExpression(toExpr(o.expr), "BETWEEN", toExpr(o.lower), toExpr(o.upper))
      case "in" =>
        InArrayOQLExpression(toExpr(o.left), o.op.asInstanceOf[String], arr(o.values) map toExpr)
      case "exists" =>
        ExistsOQLExpression(
          OQLQuery(
            Ident(o.source.asInstanceOf[String]),
            null,
            null,
            List(StarOQLProject),
            opt(o.select) map toExpr,
            None,
            None,
            None,
            None,
          ),
        )
      case "grouped"  => GroupedOQLExpression(toExpr(o.expr))
      case "apply"    => ApplyOQLExpression(Ident(o.f.asInstanceOf[String]), arr(o.args) map toExpr)
      case "star"     => StarOQLExpression
      case "subquery" => QueryOQLExpression(toQuery(o.query))
      case "str"      => StringOQLExpression(o.v.asInstanceOf[String])
      case "int"      => IntegerOQLExpression(o.v.asInstanceOf[Int])
      case "float"    => FloatOQLExpression(o.v.asInstanceOf[Double])
      case "bool"     => BooleanOQLExpression(o.v.asInstanceOf[String])
      case other      => sys.error(s"fromJS: unknown expression kind '$other'")
    }

}
