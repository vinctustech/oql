package com.vinctus.oql2

import com.vinctus.oql2.StarOQLProject.label
import org.checkerframework.checker.units.qual.{g, s}
import sun.jvm.hotspot.HelloWorld.e

import scala.Seq
import scala.util.matching.Regex
import scala.util.parsing.combinator.{PackratParsers, RegexParsers}
import scala.util.parsing.input.{CharSequenceReader, Position, Positional}

object OQLParser extends RegexParsers with PackratParsers {

  lazy val pos: PackratParser[Position] = positioned(success(new Positional {})) ^^ (_.pos)

  def kw(s: String): Regex = (s"(?i)$s\\b").r

  lazy val command: PackratParser[OQLAST] = query //| insert

  lazy val query: PackratParser[OQLQuery] =
    entityName ~ project ~ opt("[" ~> logicalExpression <~ "]") ~ opt(group) ~ opt(order) ~ restrict ^^ {
      case e ~ p ~ s ~ g ~ o ~ Seq(lim, off) => OQLQuery(e, null, null, p, s, g, o, lim, off)
    }

  lazy val project: PackratParser[List[OQLProject]] =
    "{" ~ "*" ~ subtracts ~ rep1(attributeProject) ~ "}" ^^ {
      case _ ~ _ ~ s ~ ps ~ _ => s.prepended(StarOQLProject).appendedAll(ps)
    }

  lazy val subtracts: PackratParser[List[OQLProject]] = rep("-" ~> attributeName ^^ SubtractOQLProject)

  lazy val attributeProject: PackratParser[OQLProject] =
    opt(label) ~ ident ~ "(" ~ argument ~ ")" ^^ {
      case None ~ f ~ _ ~ StarOQLExpression ~ _                       => ExpressionOQLProject(f, ApplyOQLExpression(f, List(StarOQLExpression)))
      case None ~ f ~ _ ~ (a @ AttributeOQLExpression(ids, _)) ~ _    => ExpressionOQLProject(Ident(ids.head.s, ids.head.pos), a)
      case Some(l) ~ f ~ _ ~ StarOQLExpression ~ _                    => ExpressionOQLProject(l, StarOQLExpression)
      case Some(l) ~ f ~ _ ~ (a @ AttributeOQLExpression(ids, _)) ~ _ => ExpressionOQLProject(l, a)
    }

  lazy val label: OQLParser.Parser[Ident] = ident <~ ":"

  lazy val argument: PackratParser[OQLExpression] = attributeExpression | starExpression

  lazy val entityName: PackratParser[Ident] = ident

  lazy val attributeName: PackratParser[Ident] = ident

  lazy val attributeExpression: PackratParser[OQLExpression] = ident ^^ (id => AttributeOQLExpression(List(id)))

  lazy val starExpression: PackratParser[OQLExpression] = "*" ^^^ StarOQLExpression

  lazy val group: PackratParser[List[OQLExpression]] = "/" ~> expressions <~ "/"

  lazy val order: PackratParser[List[OQLOrdering]] = "<" ~> rep1sep(ordering, ",") <~ ">"

  lazy val ordering: PackratParser[OQLOrdering] =
    expression ~ opt("ASC" | "DESC") ~ opt("NULLS" ~> ("FIRST" | "LAST")) ^^ {
      case e ~ d ~ n =>
        OQLOrdering(
          e,
          (d, n) match {
            case (None, None) | (Some("ASC"), None) => "ASC NULLS FIRST"
            case (None, Some(nulls))                => s"ASC NULLS ${nulls.toUpperCase}"
            case (_, None)                          => "DESC NULLS LAST"
            case (Some(dir), Some(nulls))           => s"${dir.toUpperCase} NULLS ${nulls.toUpperCase}"
          }
        )
    }

  lazy val restrict: PackratParser[Seq[Option[Int]]] =
    "|" ~> integer ~ opt("," ~> integer) <~ "|" ^^ { case l ~ o => Seq(Some(l), o) } |
      "|" ~> "," ~> integer <~ "|" ^^ (o => Seq(None, Some(o))) |
      success(Seq(None, None))

  lazy val expressions: PackratParser[List[OQLExpression]] = rep1sep(expression, ",")

  lazy val logicalExpression: PackratParser[OQLExpression] = additive

  lazy val expression: PackratParser[OQLExpression] = additive

  lazy val additive: PackratParser[OQLExpression] =
    additive ~ ("+" | "-") ~ multiplicative ^^ {
      case l ~ o ~ r => InfixOQLExpression(l, o, r)
    } |
      multiplicative

  lazy val multiplicative: PackratParser[OQLExpression] =
    multiplicative ~ ("*" | "/") ~ primary ^^ {
      case l ~ o ~ r => InfixOQLExpression(l, o, r)
    } |
      primary

  lazy val primary: PackratParser[OQLExpression] =
    pos ~ integer ^^ { case p ~ n => IntegerOQLExpression(n, p) } |
      pos ~ float ^^ { case p ~ n => FloatOQLExpression(n, p) }

  lazy val float: PackratParser[Double] = """[0-9]*\.[0-9]+([eE][+-]?[0-9]+)?""".r ^^ (_.toDouble)

  lazy val integer: PackratParser[Int] = "[0-9]+".r ^^ (_.toInt)

  lazy val ident: PackratParser[Ident] =
    pos ~ """[a-zA-Z_$][a-zA-Z0-9_$]*""".r ^^ {
      case p ~ s => Ident(s, p)
    }

  def parseQuery(input: String): OQLQuery =
    parseAll(phrase(query), new PackratReader(new CharSequenceReader(input))) match {
      case Success(result, _)     => result
      case NoSuccess(error, rest) => sys.error(s"$error: ${rest.pos}")
    }

}
