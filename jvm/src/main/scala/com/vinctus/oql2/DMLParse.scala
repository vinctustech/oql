package com.vinctus.oql2

import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}

import java.io.ByteArrayInputStream
import scala.jdk.CollectionConverters._

object DMLParse {

  def apply(input: String): DMLAST = {
    val charStream = CharStreams.fromStream(new ByteArrayInputStream(input.getBytes))
    val lexer = new DMLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new DMLParser(tokens)
    val visitor = new DMLASTVisitor()

    visitor.visit(parser.model())
  }

}

class DMLASTVisitor extends DMLBaseVisitor[DMLAST] {

  override def visitModel(ctx: DMLParser.ModelContext): DMLModel = {
    DMLModel(ctx.entity().asScala.toList map visitEntity)
  }

  override def visitEntity(ctx: DMLParser.EntityContext): DMLEntity = {
    DMLEntity(Ident(ctx.Ident().getSymbol, ctx.Ident().getText))
  }

}
