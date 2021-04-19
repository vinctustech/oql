package com.vinctus.oql2

import com.vinctus.oql2.DMLParser.{AliasContext, AttributeNameContext}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ConsoleErrorListener}

object DMLParse {

  def apply(input: String): Option[DMLModel] = {
    val charStream = CharStreams.fromString(input)
    val lexer = new DMLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new DMLParser(tokens)
    val errors = new ErrorListener(input)

    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
    parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
    lexer.addErrorListener(errors)
    parser.addErrorListener(errors)
    parsingError = false

    val res = parser.model.m

    if (parsingError) None
    else Some(res)
  }

  def alias(ctx: AliasContext): Option[Ident] = if (ctx eq null) None else Some(ctx.id)

  def attributeName(ctx: AttributeNameContext): Option[Ident] = if (ctx eq null) None else Some(ctx.id)

}
