package com.vinctus.oql2

import com.vinctus.oql2.OQLParser.{
  ExpressionContext,
  GroupContext,
  LabelContext,
  OrderContext,
  ProjectContext,
  SelectContext,
  WhenContext
}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ConsoleErrorListener, ParserRuleContext}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object OQLParse {

  def apply(input: String): Option[OQLAST] = {
    val charStream = CharStreams.fromString(input)
    val lexer = new OQLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new OQLParser(tokens)
    val errors = new ErrorListener(input)

    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
    parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
    lexer.addErrorListener(errors)
    parser.addErrorListener(errors)
    error = false

    val res = parser.command

    if (error) None
    else Some(res.c)
  }

  def label(ctx: LabelContext): Option[Ident] = if (ctx eq null) None else Some(ctx.id)

  def project(ps: mutable.Buffer[OQLProject]): List[OQLProject] = if (ps eq null) Nil else ps.toList

  def select(ctx: SelectContext): Option[OQLExpression] = if (ctx eq null) None else Some(ctx.e)

  def group(ctx: GroupContext): Option[List[AttributeOQLExpression]] = if (ctx eq null) None else Some(ctx.es.toList)

  def order(ctx: OrderContext): Option[List[OQLOrdering]] = if (ctx eq null) None else Some(ctx.os.toList)

  def ordering(dir: String, nulls: String): String =
    (dir, nulls) match {
      case (null, null) | ("ASC" | "asc", null) => "ASC NULLS FIRST"
      case (null, nulls) if nulls ne null       => s"ASC NULLS ${nulls.toUpperCase}"
      case (_, null)                            => "DESC NULLS LAST"
      case (dir, nulls)                         => s"${dir.toUpperCase} NULLS ${nulls.toUpperCase}"
    }

  def restrict(limit: String, offset: String): OQLRestrict =
    OQLRestrict(Option(limit) map (_.toInt), Option(offset) map (_.toInt))

  def caseExpression(whens: java.util.List[WhenContext], els: ExpressionContext): OQLExpression =
    CaseOQLExpression(whens.asScala.toList map (_.w), Option(els) map (_.e))

  val star: List[OQLExpression] = List(StarOQLExpression)

}
