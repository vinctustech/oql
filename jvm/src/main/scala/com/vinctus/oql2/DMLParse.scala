package com.vinctus.oql2

import com.vinctus.oql2.DMLParser.AliasContext
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.{
  ANTLRErrorListener,
  CharStreams,
  CommonTokenStream,
  Parser,
  RecognitionException,
  Recognizer
}

import java.util

object DMLParse {

  def apply(input: String): Option[DMLAST] = {
    val charStream = CharStreams.fromString(input)
    val lexer = new DMLLexer(charStream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new DMLParser(tokens)
    val errors = new ErrorListener(input)

    parser.addErrorListener(errors)

    val res = parser.model.m

    if (errors.error) None
    else Some(res)
  }

  def alias(ctx: AliasContext): Option[Ident] = if (ctx eq null) None else Some(ctx.id)

}

class ErrorListener(input: String) extends ANTLRErrorListener {
  var error: Boolean = false

  def syntaxError(recognizer: Recognizer[_, _],
                  offendingSymbol: Any,
                  line: Int,
                  charPositionInLine: Int,
                  msg: String,
                  e: RecognitionException): Unit = {
    Console.err.println(s"error on line $line, column ${charPositionInLine + 1}: $msg")
    Console.err.println("  " ++ io.Source.fromString(input).getLines().drop(line - 1).next())
    Console.err.println("  " ++ " " * charPositionInLine :+ '^')
    error = true
  }

  def reportAmbiguity(recognizer: Parser,
                      dfa: DFA,
                      startIndex: Int,
                      stopIndex: Int,
                      exact: Boolean,
                      ambigAlts: util.BitSet,
                      configs: ATNConfigSet): Unit = {
    Console.err.println("reportAmbiguity")
  }

  def reportAttemptingFullContext(recognizer: Parser,
                                  dfa: DFA,
                                  startIndex: Int,
                                  stopIndex: Int,
                                  conflictingAlts: util.BitSet,
                                  configs: ATNConfigSet): Unit = {
    Console.err.println("reportAttemptingFullContext")
  }

  def reportContextSensitivity(recognizer: Parser,
                               dfa: DFA,
                               startIndex: Int,
                               stopIndex: Int,
                               prediction: Int,
                               configs: ATNConfigSet): Unit = {
    Console.err.println("reportContextSensitivity")
  }
}
