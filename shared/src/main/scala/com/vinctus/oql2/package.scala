package com.vinctus

package object oql2 {

  var error: Boolean = _

  def printError(pos: Position, msg: String, input: String): Null = printError(pos.line, pos.col, msg, input)

  def printError(line: Int, charPositionInLine: Int, msg: String, input: String): Null = {
    Console.err.println(s"error on line $line, column ${charPositionInLine + 1}: $msg")
    Console.err.println("  " ++ io.Source.fromString(input).getLines().drop(line - 1).next())
    Console.err.println("  " ++ " " * charPositionInLine :+ '^')
    error = true
    null
  }

  type OBJECT = Map[String, Any]

}
