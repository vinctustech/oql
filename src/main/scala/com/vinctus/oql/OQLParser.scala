package com.vinctus.oql

import scala.util.matching.Regex
import scala.util.parsing.combinator.{PackratParsers, RegexParsers}
import scala.util.parsing.input.{CharSequenceReader, Position, Positional}

object OQLParser extends RegexParsers with PackratParsers {

  lazy val pos: PackratParser[Position] = positioned(success(new Positional {})) ^^ (_.pos)

  def kw(s: String): Regex = (s"(?i)$s\\b").r

  lazy val command: PackratParser[OQLAST] = query //| insert
//
//  lazy val insert: PackratParser[OQLInsert] = entityName ~ "<-" ~ rep1sep(value, ",") ^^ { case e ~ _ ~ vs => OQLInsert(e, vs) }
//
//  lazy val value: PackratParser[Seq[OQLKeyValue]] = "{" ~> rep1(pair) <~ "}"
//
//  lazy val pair: PackratParser[OQLKeyValue] = identifier ~ ":" ~ literalExpression ^^ { case k ~ _ ~ v => OQLKeyValue(k, v) }

  lazy val query: PackratParser[OQLQuery] =
    entityName ~ project ~ opt(select) ~ opt(group) ~ opt(order) ~ restrict ^^ {
      case e ~ p ~ s ~ g ~ o ~ Seq(lim, off) => OQLQuery(e, null, null, p, s, g, o, lim, off)
    }

  lazy val project: PackratParser[List[OQLProject]] =
    "{" ~ "*" ~ subtracts ~ rep(attributeProject) ~ "}" ^^ {
      case _ ~ _ ~ s ~ ps ~ _ => s.prepended(StarOQLProject).appendedAll(ps)
    } |
      "{" ~> rep1(attributeProject) <~ "}" |
      success(List(StarOQLProject))

  lazy val subtracts: PackratParser[List[OQLProject]] = rep("-" ~> attributeName ^^ SubtractOQLProject)

  lazy val attributeProject: PackratParser[OQLProject] =
    opt(label) ~ identifier ~ "(" ~ argument ~ ")" ^^ {
      case None ~ f ~ _ ~ StarOQLExpression ~ _ => ExpressionOQLProject(f, ApplyOQLExpression(f, List(StarOQLExpression)))
      case None ~ f ~ _ ~ (a @ AttributeOQLExpression(ids, _)) ~ _ =>
        ExpressionOQLProject(Ident(s"${f.s}_${ids.head.s}", f.pos), ApplyOQLExpression(f, List(a)))
      case Some(l) ~ f ~ _ ~ StarOQLExpression ~ _                    => ExpressionOQLProject(l, ApplyOQLExpression(f, List(StarOQLExpression)))
      case Some(l) ~ f ~ _ ~ (a @ AttributeOQLExpression(ids, _)) ~ _ => ExpressionOQLProject(l, ApplyOQLExpression(f, List(a)))
    } |
      label ~ applyExpression ^^ { case l ~ e              => ExpressionOQLProject(l, e) } |
      label ~ qualifiedAttributeExpression ^^ { case l ~ e => ExpressionOQLProject(l, e) } |
      label ~ ("(" ~> expression <~ ")") ^^ { case l ~ e   => ExpressionOQLProject(l, e) } |
      opt(label) ~ query ^^ {
        case None ~ q    => QueryOQLProject(q.source, q)
        case Some(l) ~ q => QueryOQLProject(l, q)
      } |
      opt(label) ~ attributeExpression ^^ {
        case None ~ a    => ExpressionOQLProject(a.ids.head, a)
        case Some(l) ~ a => ExpressionOQLProject(l, a)
      } |
      opt(label) ~ ("&" ~> attributeName) ^^ {
        case None ~ a    => ExpressionOQLProject(a, ReferenceOQLExpression(List(a)))
        case Some(l) ~ a => ExpressionOQLProject(l, ReferenceOQLExpression(List(a)))
      }

  lazy val label: PackratParser[Ident] = identifier <~ ":"

  lazy val argument: PackratParser[OQLExpression] = attributeExpression | starExpression

  lazy val entityName: PackratParser[Ident] = identifier

  lazy val attributeName: PackratParser[Ident] = identifier

  lazy val applyExpression: PackratParser[OQLExpression] =
    identifier ~ ("(" ~> expressions <~ ")") ^^ { case f ~ as => ApplyOQLExpression(f, as) }

  lazy val attributeExpression: PackratParser[AttributeOQLExpression] = identifier ^^ (id => AttributeOQLExpression(List(id)))

  lazy val qualifiedAttributeExpression: PackratParser[OQLExpression] =
    identifiers ^^ (ids => AttributeOQLExpression(ids))

  lazy val identifiers: PackratParser[List[Ident]] = rep1sep(attributeName, ".")

  lazy val starExpression: PackratParser[OQLExpression] = "*" ^^^ StarOQLExpression

  lazy val select: PackratParser[OQLExpression] = "[" ~> booleanExpression <~ "]"

  lazy val group: PackratParser[List[OQLExpression]] = "/" ~> expressions <~ "/"

  lazy val order: PackratParser[List[OQLOrdering]] = "<" ~> rep1sep(ordering, ",") <~ ">"

  lazy val ordering: PackratParser[OQLOrdering] =
    expression ~ opt(kw("ASC") | kw("DESC")) ~ opt(kw("NULLS") ~> (kw("FIRST") | kw("LAST"))) ^^ {
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

  lazy val booleanExpression: PackratParser[OQLExpression] = orExpression

  lazy val orExpression: PackratParser[OQLExpression] =
    orExpression ~ kw("OR") ~ andExpression ^^ { case l ~ _ ~ r => InfixOQLExpression(l, "OR", r) } |
      andExpression

  lazy val andExpression: PackratParser[OQLExpression] =
    andExpression ~ kw("AND") ~ notExpression ^^ { case l ~ _ ~ r => InfixOQLExpression(l, "AND", r) } |
      notExpression

  lazy val notExpression: PackratParser[OQLExpression] =
    kw("NOT") ~> booleanPrimary ^^ (e => PrefixOQLExpression("NOT", e)) | booleanPrimary

  lazy val booleanPrimary: PackratParser[OQLExpression] =
    expression ~ comparison ~ expression ^^ { case l ~ c ~ r => InfixOQLExpression(l, c, r) } |
      expression ~ ((kw("NOT") ~ kw("BETWEEN") ^^^ "NOT BETWEEN") | kw("BETWEEN")) ~ expression ~ kw("AND") ~ expression ^^ {
        case e ~ b ~ l ~ _ ~ u => BetweenOQLExpression(e, b, l, u)
      } |
      expression ~ isNull ^^ { case e ~ n                                => PostfixOQLExpression(e, n) } |
      expression ~ in ~ ("(" ~> expressions <~ ")") ^^ { case e ~ i ~ es => InArrayOQLExpression(e, i, es) } |
      expression ~ in ~ ("(" ~> query <~ ")") ^^ { case e ~ i ~ q        => InQueryOQLExpression(e, i, q) } |
      kw("EXISTS") ~> "(" ~> query <~ ")" ^^ ExistsOQLExpression |
      booleanLiteral |
      qualifiedAttributeExpression |
      "(" ~> booleanExpression <~ ")" ^^ GroupedOQLExpression

  lazy val isNull: PackratParser[String] = kw("IS") ~ kw("NULL") ^^^ "IS NULL" | kw("IS") ~ kw("NOT") ~ kw("NULL") ^^^ "IS NOT NULL"

  lazy val in: PackratParser[String] = kw("NOT") ~ kw("IN") ^^^ "NOT IN" | kw("IN")

  lazy val comparison: PackratParser[String] =
    "<=" | ">=" | "<" | ">" | "=" | "!=" | kw("LIKE") | kw("ILIKE") | (kw("NOT") ~ kw("LIKE") ^^^ "NOT LIKE") | (kw("NOT") ~ kw("ILIKE") ^^^ "NOT ILIKE")

  lazy val booleanLiteral: PackratParser[OQLExpression] = (kw("TRUE") | kw("FALSE")) ^^ BooleanOQLExpression

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

  lazy val literalExpression: PackratParser[OQLExpression] =
    float ^^ FloatOQLExpression |
      integer ^^ IntegerOQLExpression |
      string ^^ LiteralOQLExpression |
      booleanLiteral

  lazy val primary: PackratParser[OQLExpression] =
    literalExpression |
      starExpression |
      caseExpression |
      applyExpression |
      qualifiedAttributeExpression |
      "&" ~> identifiers ^^ (ReferenceOQLExpression(_)) |
      "-" ~> primary ^^ (e => PrefixOQLExpression("-", e)) |
      "(" ~> query <~ ")" ^^ QueryOQLExpression |
      "(" ~> expression <~ ")" ^^ GroupedOQLExpression

  lazy val caseExpression: PackratParser[CaseOQLExpression] =
    kw("CASE") ~> rep1(when) ~ opt(kw("ELSE") ~> expression) <~ kw("END") ^^ { case ws ~ e => CaseOQLExpression(ws, e) }

  lazy val when: PackratParser[OQLWhen] =
    kw("WHEN") ~ booleanExpression ~ kw("THEN") ~ expression ^^ { case _ ~ l ~ _ ~ e => OQLWhen(l, e) }

  lazy val float: PackratParser[Double] = """[0-9]*\.[0-9]+([eE][+-]?[0-9]+)?""".r ^^ (_.toDouble)

  lazy val integer: PackratParser[Int] = "[0-9]+".r ^^ (_.toInt)

  lazy val identifier: PackratParser[Ident] =
    pos ~ """[a-zA-Z_$][a-zA-Z0-9_$]*""".r ^^ {
      case p ~ s => Ident(s, p)
    }

  lazy val singleQuoteString: PackratParser[String] =
    """'(?:[^'\x00-\x1F\x7F\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*'""".r ^^ (s => s.substring(1, s.length - 1))

  lazy val doubleQuoteString: PackratParser[String] =
    """"(?:[^"\x00-\x1F\x7F\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*"""".r ^^ (s => s.substring(1, s.length - 1))

  lazy val string: PackratParser[String] = singleQuoteString | doubleQuoteString

  def parseQuery(input: String): OQLQuery =
    parseAll(phrase(query), new PackratReader(new CharSequenceReader(input))) match {
      case Success(result, _)     => result
      case NoSuccess(error, rest) => problem(rest.pos, error, input)
    }

  def parseBooleanExpression(input: String): OQLExpression =
    parseAll(phrase(booleanExpression), new PackratReader(new CharSequenceReader(input))) match {
      case Success(result, _)     => result
      case NoSuccess(error, rest) => problem(rest.pos, error, input)
    }

}
