package com.vinctus.oql2

import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{CharSequenceReader, Position, Positional}

class DMLParser extends RegexParsers {

  override protected val whiteSpace: Regex = """(\s|//.*)+""".r

  def pos: Parser[Position] = positioned(success(new Positional {})) ^^ (_.pos)

  def integer: Parser[String] = "[0-9]+".r

  def ident: Parser[Ident] =
    pos ~ """[a-zA-Z_$][a-zA-Z0-9_$]*""".r ^^ {
      case p ~ s => Ident(s, p)
    }

  def model: Parser[DMLModel] = rep1(entity) ^^ DMLModel

  def entity: Parser[DMLEntity] =
    "entity" ~ ident ~ opt("(" ~> ident <~ ")") ~ "{" ~ rep1(attribute) ~ "}" ^^ {
      case _ ~ n ~ a ~ _ ~ as ~ _ =>
        DMLEntity(n, a, as)
    }

  def attribute: Parser[DMLAttribute] =
    opt("*") ~ ident ~ opt("(" ~> ident <~ ")") ~ ":" ~ typeSpecifier ~ opt("!") ^^ {
      case pk ~ n ~ a ~ _ ~ t ~ r =>
        DMLAttribute(n, a, t, pk isDefined, r isDefined)
    }

  def typeSpecifier: Parser[DMLTypeSpecifier] =
    ("text" |
      "integer" | "int" | "int4" |
      "bool" | "boolean" |
      "bigint" |
      "date" |
      "float" | "float8" |
      "uuid" |
      "timestamp") ^^ DMLSimpleDataType |
      "decimal" ~ "(" ~ integer ~ "," ~ integer ~ ")" ^^ {
        case _ ~ _ ~ p ~ _ ~ s ~ _ => DMLParametricDataType("decimal", List(p, s))
      } |
      ident ^^ DMLManyToOneType |
      "[" ~ ident ~ "]" ~ opt("." ~> ident) ^^ {
        case _ ~ n ~ _ ~ t => DMLOneToManyType(n, t)
      } |
      "<" ~ ident ~ ">" ~ opt("." ~> ident) ^^ {
        case _ ~ n ~ _ ~ t => DMLOneToOneType(n, t)
      } |
      "[" ~ ident ~ "]" ~ "(" ~ ident ~ ")" ^^ {
        case _ ~ n ~ _ ~ _ ~ l ~ _ => DMLManyToManyType(n, l)
      }

  def parseFromString[T](src: String, grammar: Parser[T]): T =
    parseAll(grammar, new CharSequenceReader(src)) match {
      case Success(tree, _)       => tree
      case NoSuccess(error, rest) => problem(rest.pos, error)
    }

}
