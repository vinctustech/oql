package com.vinctus.oql2

import com.vinctus.oql2.StarOQLProject.label

import scala.util.matching.Regex
import scala.util.parsing.combinator.{PackratParsers, RegexParsers}
import scala.util.parsing.input.{CharSequenceReader, Position, Positional}

object OQLParser extends RegexParsers with PackratParsers {

  lazy val pos: PackratParser[Position] = positioned(success(new Positional {})) ^^ (_.pos)

  def kw(s: String): Regex = (s"(?i)$s\\b").r

  lazy val command: PackratParser[OQLAST] = query | insert

  lazy val query: PackratParser[OQLQuery] =
    entityName ~ project ~ opt("[" ~> logicalExpression <~ "]") ~ opt(group) ~ opt(order) ~ restrict ^^ {
      case e ~ p ~ s ~ g ~ o ~ r => OQLQuery(e, null, null, p, s, g, o, r.limit, r.offset)
    }

  lazy val project: PackratParser[List[OQLProject]] = {
  "{" ~ "*" ~ subtracts ~ attributeProjects ~ "}" ^^ {
    case _ ~ _ ~ s ~ ps ~ _ => s.prepend(StarOQLProject).appendAll(ps)
  }

  lazy val subtracts: PackratParser[List[OQLProject]] = rep("-" ~> attributeName ^^ SubtractOQLProject)

    lazy val attributeProject: PackratParser[OQLProject] =
      opt(label) ~ ident ~ "(" ~ argument

    lazy val label = ident <~ ":"

    lazy val argument: PackratParser[OQLExpression] =
      ident ^^

    lazy val entityName: PackratParser[Ident] = ident

    lazy val attributeName: PackratParser[Ident] = ident

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
    "[0-9]+".r ^^ NumberExpression |
      kw("exp") ~> primary ^^ ExpExpression |
      ident ^^ VariableExpression

  lazy val ident: PackratParser[Ident] =
    pos ~ """[a-zA-Z_$][a-zA-Z0-9_$]*""".r ^^ {
      case p ~ s => Ident(s, p)
    }

  def parseQuery(input: String): OQLQuery =
    parseAll(phrase(expression), new PackratReader(new CharSequenceReader(input))) match {
      case Success(result, _)     => result
      case NoSuccess(error, rest) => sys.error(s"$error: ${rest.pos}")
    }

}
