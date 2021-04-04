package com.vinctus.oql2

import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.{ANTLRErrorListener, Parser, RecognitionException, Recognizer}

import java.util

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