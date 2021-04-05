package com.vinctus.oql2

import com.vinctus.oql2.OQLParser.{GroupContext, LabelContext, ProjectContext, SelectContext}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ConsoleErrorListener}

import scala.collection.mutable

object OQLParse {

  def apply(input: String): Option[OQLQuery] = {
    val charStream = CharStreams.fromString(input)
    val lexer = new OQLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new OQLParser(tokens)
    val errors = new ErrorListener(input)

    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
    parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
    lexer.addErrorListener(errors)
    parser.addErrorListener(errors)

    val res = parser.query

    if (errors.error) None
    else Some(res.q)
  }

  def label(ctx: LabelContext): Option[Ident] = if (ctx eq null) None else Some(ctx.id)

  def project(ps: mutable.Buffer[OQLProject]): List[OQLProject] = if (ps eq null) Nil else ps.toList

  def select(ctx: SelectContext): Option[OQLExpression] = if (ctx eq null) None else Some(ctx.e)

  def group(ctx: GroupContext): Option[List[AttributeOQLExpression]] = if (ctx eq null) None else Some(ctx.es.toList)

  val star: List[OQLExpression] = List(StarOQLExpression)

}
