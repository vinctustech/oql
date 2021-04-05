package com.vinctus

package object oql2 {

  def printError(line: Int, charPositionInLine: Int, msg: String, input: String): Unit = {
    Console.err.println(s"error on line $line, column ${charPositionInLine + 1}: $msg")
    Console.err.println("  " ++ io.Source.fromString(input).getLines().drop(line - 1).next())
    Console.err.println("  " ++ " " * charPositionInLine :+ '^')
  }

}
