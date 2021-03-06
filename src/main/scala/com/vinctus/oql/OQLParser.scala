package com.vinctus.oql

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{CharSequenceReader, Position, Positional}

object OQLParser {

  def parseQuery(query: String): QueryOQL = {
    val p = new OQLParser

    p.parseFromString(query, p.query)
  }

  def parseSelect(s: String): ExpressionOQL = {
    val p = new OQLParser

    p.parseFromString(s, p.logicalExpression)
  }

  def parseGroup(s: String): Seq[VariableExpressionOQL] = {
    val p = new OQLParser

    p.parseFromString(s, p.variables)
  }

  def parseOrder(s: String): Seq[(ExpressionOQL, String)] = {
    val p = new OQLParser

    p.parseFromString(s, p.orderExpressions)
  }

}

class OQLParser extends RegexParsers {

  override protected val whiteSpace: Regex = """(\s|//.*)+""".r

  def pos: Parser[Position] = positioned(success(new Positional {})) ^^ {
    _.pos
  }

  def kw(s: String): Regex = s"(?i)$s\\b".r

  def integer: Parser[IntegerLiteralOQL] =
    positioned("""\d+""".r ^^ IntegerLiteralOQL)

  def number: Parser[ExpressionOQL] =
    positioned("""-?\d+(\.\d*)?""".r ^^ {
      case n if n contains '.' => FloatLiteralOQL(n)
      case n                   => IntegerLiteralOQL(n)
    })

  def singleQuoteString: Parser[String] =
    ("""'(?:''|[^'\x00-\x1F\x7F\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*'""".r) ^^ (s => s.substring(1, s.length - 1))

  def doubleQuoteString: Parser[String] =
    (""""(?:""|[^"\x00-\x1F\x7F\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*"""".r) ^^ (s => s.substring(1, s.length - 1))

  def string: Parser[StringLiteralOQL] =
    positioned((singleQuoteString | doubleQuoteString) ^^ StringLiteralOQL)

  def ident: Parser[Ident] =
    positioned("""[a-zA-Z_$][a-zA-Z0-9_$]*""".r ^^ Ident)

  def identOrStar: Parser[Ident] =
    positioned("""(?:[a-zA-Z_$][a-zA-Z0-9_$]*)|\*""".r ^^ Ident)

  def star: Parser[Ident] = positioned("*" ^^ Ident)

  def query: Parser[QueryOQL] =
    ident ~ opt(project) ~ opt(select) ~ opt(expressions) ~ opt(order) ~ opt(restrict) ^^ {
      case e ~ p ~ s ~ g ~ o ~ r =>
        QueryOQL(e,
                 if (p isDefined) p.get else ProjectAllOQL(),
                 s,
                 g,
                 o,
                 if (r isDefined) r.get._1 else None,
                 if (r isDefined) r.get._2 else None)
    }

  def project: Parser[ProjectExpressionOQL] =
    "{" ~> rep1(
      attributeProject | "-" ~> ident ^^ NegativeAttribute | "&" ~> ident ^^ ReferenceAttributeOQL | star ^^ (s =>
        ProjectAllOQL(s.pos))) <~ "}" ^^ ProjectAttributesOQL |
      "." ~> attributeProject ^^ {
        case a: ProjectAllOQL => a
        case a                => ProjectAttributesOQL(List(a))
      }

  def attributeProject: Parser[ProjectExpressionOQL] =
    applyAttribute ^^ {
      case (fs, id) => AggregateAttributeOQL(fs, id)
    } |
      "^" ~> query ^^ LiftedAttribute |
      query

  def applyAttribute: Parser[(List[Ident], Ident)] =
    ident ~ ("(" ~> (applyAttribute | identOrStar) <~ ")") ^^ { r =>
      def result(p: Any): (List[Ident], Ident) =
        p match {
          case (f: Ident) ~ (a: Ident)                => (List(f), a)
          case (f: Ident) ~ ((fs: List[_], a: Ident)) => (f +: fs.asInstanceOf[List[Ident]], a)
        }

      result(r)
    }

  def variable: Parser[VariableExpressionOQL] = rep1sep(ident, ".") ^^ VariableExpressionOQL

  def expression: Parser[ExpressionOQL] = additiveExpression

  def logicalExpression: Parser[ExpressionOQL] =
    orExpression

  def orExpression: Parser[ExpressionOQL] =
    andExpression ~ rep(kw("OR") ~> andExpression) ^^ {
      case expr ~ list =>
        list.foldLeft(expr) {
          case (l, r) => InfixExpressionOQL(l, "OR", r)
        }
    }

  def andExpression: Parser[ExpressionOQL] =
    notExpression ~ rep(kw("AND") ~> notExpression) ^^ {
      case expr ~ list =>
        list.foldLeft(expr) {
          case (l, r) => InfixExpressionOQL(l, "AND", r)
        }
    }

  def notExpression: Parser[ExpressionOQL] =
    kw("NOT") ~> comparisonExpression ^^ (p => PrefixExpressionOQL("NOT", p)) |
      comparisonExpression

  def comparisonExpression: Parser[ExpressionOQL] =
    kw("EXISTS") ~> "(" ~> query <~ ")" ^^ ExistsExpressionOQL |
      additiveExpression ~ ("<=" | ">=" | "<" | ">" | "=" | "!=" | kw("LIKE") | kw("ILIKE") | (kw("NOT") ~ kw("LIKE") ^^^ "NOT LIKE") | (kw(
        "NOT") ~ kw("ILIKE") ^^^ "NOT ILIKE")) ~ additiveExpression ^^ {
        case l ~ o ~ r => InfixExpressionOQL(l, o, r)
      } |
      additiveExpression ~ opt(kw("NOT")) ~ kw("BETWEEN") ~ additiveExpression ~ kw("AND") ~ additiveExpression ^^ {
        case a ~ None ~ _ ~ b ~ _ ~ c    => BetweenExpressionOQL(a, "BETWEEN", b, c)
        case a ~ Some(_) ~ _ ~ b ~ _ ~ c => BetweenExpressionOQL(a, "NOT BETWEEN", b, c)
      } |
      additiveExpression ~ ((kw("IS") ~ kw("NULL") ^^^ "IS NULL") | (kw("IS") ~ kw("NOT") ~ kw("NULL")) ^^^ "IS NOT NULL") ^^ {
        case l ~ o => PostfixExpressionOQL(l, o)
      } |
      additiveExpression ~ (kw("IN") ^^^ "IN" | kw("NOT") ~ kw("IN") ^^^ "NOT IN") ~ expressions ^^ {
        case e ~ o ~ l => InExpressionOQL(e, o, l)
      } |
      additiveExpression ~ (kw("IN") ^^^ "IN" | kw("NOT") ~ kw("IN") ^^^ "NOT IN") ~ ("(" ~> query <~ ")") ^^ {
        case e ~ o ~ q => InSubqueryExpressionOQL(e, o, q)
      } |
      additiveExpression

  def additiveExpression: Parser[ExpressionOQL] =
    multiplicativeExpression ~ rep(("+" | "-") ~ multiplicativeExpression) ^^ {
      case e ~ l =>
        l.foldLeft(e) {
          case (x, op ~ y) => InfixExpressionOQL(x, op, y)
        }
    }

  def multiplicativeExpression: Parser[ExpressionOQL] =
    applyExpression ~ rep(("*" | "/") ~ applyExpression) ^^ {
      case e ~ l =>
        l.foldLeft(e) {
          case (x, op ~ y) => InfixExpressionOQL(x, op, y)
        }
    }

  def expressions: Parser[List[ExpressionOQL]] = "(" ~> rep1sep(expression, ",") <~ ")"

  def applyExpression: Parser[ExpressionOQL] =
    ident ~ expressions ^^ {
      case i ~ es => ApplyExpressionOQL(i, es)
    } |
      primaryExpression

  def primaryExpression: Parser[ExpressionOQL] =
    number |
      string |
      (kw("TRUE") | kw("FALSE")) ^^ (b => BooleanLiteralOQL(b.toLowerCase == "true")) |
      "&" ~> rep1sep(ident, ".") ^^ ReferenceExpressionOQL |
      kw("INTERVAL") ~> singleQuoteString ^^ IntervalLiteralOQL |
      caseExpression |
      variable |
      "(" ~> query <~ ")" ^^ SubqueryExpressionOQL |
      "(" ~> logicalExpression <~ ")" ^^ GroupedExpressionOQL

  def caseExpression: Parser[CaseExpressionOQL] =
    kw("CASE") ~ rep1(when) ~ opt(kw("ELSE") ~> expression) ~ kw("END") ^^ {
      case _ ~ ws ~ e ~ _ => CaseExpressionOQL(ws, e)
    }

  def when: Parser[(ExpressionOQL, ExpressionOQL)] =
    kw("WHEN") ~ logicalExpression ~ kw("THEN") ~ expression ^^ {
      case _ ~ c ~ _ ~ r => (c, r)
    }

  def select: Parser[ExpressionOQL] = "[" ~> logicalExpression <~ "]"

  def order: Parser[List[(ExpressionOQL, String)]] = "<" ~> orderExpressions <~ ">"

  def orderExpressions: Parser[List[(ExpressionOQL, String)]] = rep1sep(orderExpression, ",")

  def orderExpression: Parser[(ExpressionOQL, String)] = expression ~ ordering ^^ {
    case e ~ o => (e, o)
  }

  def ordering: Parser[String] =
    opt(kw("ASC") | kw("DESC")) ~ opt(kw("NULLS") ~> (kw("FIRST") | kw("LAST"))) ^^ {
      case None ~ None | Some("ASC" | "asc") ~ None => "ASC NULLS FIRST"
      case None ~ Some(nulls)                       => s"ASC NULLS ${nulls.toUpperCase}"
      case _ ~ None                                 => "DESC NULLS LAST"
      case Some(dir) ~ Some(nulls)                  => s"${dir.toUpperCase} NULLS ${nulls.toUpperCase}"
    }

  def variables: Parser[List[VariableExpressionOQL]] = rep1sep(variable, ",")

  def restrict: Parser[(Option[Int], Option[Int])] =
    "|" ~> (integer ~ opt("," ~> integer)) <~ "|" ^^ {
      case l ~ o => (Some(l.n.toInt), o map (_.n.toInt))
    } |
      "|" ~> "," ~> integer <~ "|" ^^ (o => (None, Some(o.n.toInt)))

  def parseFromString[T](src: String, grammar: Parser[T]): T =
    parseAll(grammar, new CharSequenceReader(src)) match {
      case Success(tree, _)       => tree
      case NoSuccess(error, rest) => problem(rest.pos, error)
    }

}
