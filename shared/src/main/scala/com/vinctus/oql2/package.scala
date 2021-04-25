package com.vinctus

import scala.util.parsing.input.Position

package object oql2 {

  var parsingError: Boolean = _

  def ni: Nothing = sys.error("not implemented (yet)")

  def problem(pos: Position, msg: String, input: String): Nothing = {
    printError(pos, msg, input)
    sys.error("error executing query")
  }

  def printError(pos: Position, msg: String, input: String): Null = {
    if (pos eq null)
      Console.err.println(msg)
    else if (pos.line == 1)
      Console.err.println(s"$msg\n${pos.longString}")
    else
      Console.err.println(s"${pos.line}: $msg\n${pos.longString}")
//      printError(pos.line, pos.col, msg, input)

    null
  }

//  def printError(line: Int, charPositionInLine: Int, msg: String, input: String): Null = {
//    Console.err.println(s"error on line $line, column ${charPositionInLine + 1}: $msg")
//    Console.err.println("  " ++ io.Source.fromString(input).getLines().drop(line - 1).next())
//    Console.err.println("  " ++ " " * charPositionInLine :+ '^')
//    parsingError = true
//    null
//  }

  type OBJECT = Map[String, Any]

//  private val escapeReplaceMap = Seq(
//    "\\\\" -> "\\",
//    "\\n" -> "\n",
//    "\\r" -> "\r",
//    "\\t" -> "\t",
//    "\\\"" -> "\"",
//    "\\'" -> "'"
//  )
//
//  def escape(s: String): String = escapeReplaceMap.foldLeft(s) { case (acc, (c, r)) => acc.replace(c, r) }

  private val quoteReplaceMap = Seq(
    "'" -> "\\'",
    "\\\\'" -> "\\'"
  )

  def quote(s: String): String = quoteReplaceMap.foldLeft(s) { case (acc, (c, r)) => acc.replace(c, r) }

}
