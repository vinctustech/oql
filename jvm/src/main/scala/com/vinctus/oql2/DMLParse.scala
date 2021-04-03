package com.vinctus.oql2

import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.{
  ANTLRErrorListener,
  CharStreams,
  CommonTokenStream,
  Parser,
  ParserRuleContext,
  RecognitionException,
  Recognizer
}

import java.io.ByteArrayInputStream
import java.util
import scala.jdk.CollectionConverters._

object DMLParse extends ANTLRErrorListener {

  var error: Boolean = _

  def apply(input: String): Option[DMLAST] = {
    val charStream = CharStreams.fromStream(new ByteArrayInputStream(input.getBytes))
    val lexer = new DMLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new DMLParser(tokens)
    val visitor = new DMLASTVisitor()

    parser.addErrorListener(this)
    error = false

    val res = parser.model()

    if (error) None
    else Some(visitor.visit(res))
  }

  def syntaxError(recognizer: Recognizer[_, _],
                  offendingSymbol: Any,
                  line: Int,
                  charPositionInLine: Int,
                  msg: String,
                  e: RecognitionException): Unit = {
    error = true
  }

  def reportAmbiguity(recognizer: Parser,
                      dfa: DFA,
                      startIndex: Int,
                      stopIndex: Int,
                      exact: Boolean,
                      ambigAlts: util.BitSet,
                      configs: ATNConfigSet): Unit = {}

  def reportAttemptingFullContext(recognizer: Parser,
                                  dfa: DFA,
                                  startIndex: Int,
                                  stopIndex: Int,
                                  conflictingAlts: util.BitSet,
                                  configs: ATNConfigSet): Unit = {}

  def reportContextSensitivity(recognizer: Parser,
                               dfa: DFA,
                               startIndex: Int,
                               stopIndex: Int,
                               prediction: Int,
                               configs: ATNConfigSet): Unit = {}
}

class DMLASTVisitor extends DMLBaseVisitor[DMLAST] {

  override def visitModel(ctx: DMLParser.ModelContext): DMLModel = {
    DMLModel(ctx.entity().asScala.toList map visitEntity)
  }

  override def visitEntity(ctx: DMLParser.EntityContext): DMLEntity = {
    DMLEntity(Ident(ctx.Ident().getSymbol, ctx.Ident().getText))
  }

}
