package com.vinctus.oql

import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{CharSequenceReader, Position, Positional}

class DMLParser extends RegexParsers {

  override protected val whiteSpace: Regex = """(\s|//.*)+""".r

  def pos: Parser[Position] = positioned(success(new Positional {})) ^^ (_.pos)

  def kw(s: String): Regex = (s"(?i)$s\\b").r

  def integer: Parser[String] = "[0-9]+".r

  def ident: Parser[Ident] =
    pos ~ """[a-zA-Z_$][a-zA-Z0-9_$]*""".r ^^ {
      case p ~ s => Ident(s, p)
    }

  def model: Parser[DMLModel] = rep1(entity) ^^ DMLModel

  def entity: Parser[DMLEntity] =
    kw("entity") ~ ident ~ opt("(" ~> ident <~ ")") ~ "{" ~ rep1(attribute) ~ "}" ^^ {
      case _ ~ n ~ a ~ _ ~ as ~ _ =>
        DMLEntity(n, a, as)
    }

  def attribute: Parser[DMLAttribute] =
    opt("*") ~ ident ~ opt("(" ~> ident <~ ")") ~ ":" ~ typeSpecifier ~ opt("!") ^^ {
      case pk ~ n ~ a ~ _ ~ t ~ r =>
        DMLAttribute(n, a, t, pk isDefined, r isDefined)
    }

  def typeSpecifier: Parser[DMLTypeSpecifier] =
    (kw("text") |
      kw("integer") | kw("int4") | kw("int") |
      kw("boolean") | kw("bool") |
      kw("bigint") |
      kw("date") |
      kw("float8") | kw("float") |
      kw("uuid") |
      kw("timestamp")) ^^ DMLSimpleDataType |
      kw("decimal") ~ "(" ~ integer ~ "," ~ integer ~ ")" ^^ {
        case _ ~ _ ~ p ~ _ ~ s ~ _ => DMLParametricDataType("decimal", List(p, s))
      } |
      ident ^^ DMLManyToOneType |
      "[" ~ ident ~ "]" ~ "(" ~ ident ~ ")" ^^ {
        case _ ~ n ~ _ ~ _ ~ l ~ _ => DMLManyToManyType(n, l)
      } |
      "[" ~ ident ~ "]" ~ opt("." ~> ident) ^^ {
        case _ ~ n ~ _ ~ t => DMLOneToManyType(n, t)
      } |
      "<" ~ ident ~ ">" ~ opt("." ~> ident) ^^ {
        case _ ~ n ~ _ ~ t => DMLOneToOneType(n, t)
      }

  def parseModel(src: String): DMLModel =
    parseAll(model, new CharSequenceReader(src)) match {
      case Success(tree, _)       => tree
      case NoSuccess(error, rest) => problem(rest.pos, error, src)
    }

}
