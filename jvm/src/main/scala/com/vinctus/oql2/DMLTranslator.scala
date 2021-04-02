package com.vinctus.oql2

import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}

import java.io.ByteArrayInputStream

class DMLTranslator(input: String) {
  val charStream = CharStreams.fromStream(new ByteArrayInputStream(input.getBytes))
  val lexer = new DMLLexer(charStream)
  val tokens = new CommonTokenStream(lexer)
  val parser = new DMLParser(tokens)
  val visitor = new DMLASTVisitor()

  visitor.visit(parser.model())

}

class DMLASTVisitor extends DMLBaseVisitor[DMLAST] {}
