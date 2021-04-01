package com.vinctus.oql2

import org.parboiled2._

class DMLParser(val input: ParserInput) extends Parser {

  implicit def lit(s: String): Rule0 = rule { str(s) ~ ws }

  def ws = rule { zeroOrMore(ch(' ') | '\r' | '\n') }

//  def kw(s: String) = rule { ignoreCase(s) ~ !CharPredicate.Alpha ~ ws }

  def model = rule { ws ~ entities ~ EOI ~> DMLModel }

  def entities = rule { oneOrMore(entity) }

  def entity = rule { "entity" ~ identifier ~ "{" ~ "}" ~> DMLEntity }

  def attribute = rule {
    optional("*") ~ identifier ~ optional("(" ~ identifier ~ ")") ~ ":" ~ attributeType ~ optional("!")
  }

  def attributeType = rule { primitiveType }

  def primitiveType = rule {
    "text" |
      "integer" | "int" | "int4" |
      "bool" | "boolean" |
      "bigint" |
      "decimal" |
      "date" |
      "float" | "float8" |
      "uuid" |
      "timestamp"
  }

  def identCharFirst: CharPredicate = CharPredicate.AlphaNum ++ '_'

  def identCharRest: CharPredicate = identCharFirst ++ CharPredicate.Digit

  def identifier = rule { capture(identCharFirst ~ zeroOrMore(identCharRest)) ~ ws }

}
