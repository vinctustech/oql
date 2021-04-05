package com.vinctus.oql2

import com.vinctus.oql2.OQLParser.LabelContext
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ConsoleErrorListener}

object OQLParse {

  def apply(input: String): Option[OQLQuery] = {
    val charStream = CharStreams.fromString(input)
    val lexer = new DMLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new OQLParser(tokens)
    val errors = new ErrorListener(input)

    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
    parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
    lexer.addErrorListener(errors)
    parser.addErrorListener(errors)

    val res = parser.query.q

    if (errors.error) None
    else Some(res)
  }

  def label(ctx: LabelContext): Option[Ident] = if (ctx eq null) None else Some(ctx.id)

  val star: List[OQLExpression] = List(StarOQLExpression)

}
