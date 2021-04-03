package com.vinctus.oql2

import com.vinctus.oql2.DMLParser.AliasContext
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.TerminalNode
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
    val visitor = new DMLASTVisitor

    parser.addErrorListener(this)
    error = false

    val res = parser.model

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

  def ident(node: TerminalNode): Ident = Ident(node.getSymbol, node.getText)

  override def visitModel(ctx: DMLParser.ModelContext): DMLModel = {
    DMLModel(ctx.entity.asScala.toList map visitEntity)
  }

  def opt(node: TerminalNode): Option[TerminalNode] = if (node eq null) None else Some(node)

  def opt(ctx: AliasContext): Option[Ident] = if (ctx eq null) None else Some(ident(ctx.Ident()))

  def opt[C <: ParserRuleContext](ctx: C): Option[C] = if (ctx eq null) None else Some(ctx)

  override def visitEntity(ctx: DMLParser.EntityContext): DMLEntity = {
    DMLEntity(ident(ctx.entityName.Ident), opt(ctx.alias), ctx.attribute.asScala.toList map visitAttribute)
  }

  override def visitAttribute(ctx: DMLParser.AttributeContext): DMLAttribute = {
    DMLAttribute(
      ident(ctx.attributeName.Ident),
      opt(ctx.alias),
      visitAttributeType(ctx.attributeType).asInstanceOf[DMLTypeSpecifier],
      opt(ctx.pk).nonEmpty,
      opt(ctx.required).nonEmpty
    )
  }

  override def visitPrimitiveType(ctx: DMLParser.PrimitiveTypeContext): DMLPrimitiveType = {
    DMLPrimitiveType(ctx.getText)
  }

  override def visitManyToOneType(ctx: DMLParser.ManyToOneTypeContext): DMLManyToOneType = DMLManyToOneType(ctx.getText)
}
