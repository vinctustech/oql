package com.vinctus

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.scalajs.js
import scala.util.matching.Regex
import scala.util.parsing.input.Position

package object oql {

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

  private val HEX = {
    val a = new Array[Int](128)

    List(
      '0' -> 0,
      '1' -> 1,
      '2' -> 2,
      '3' -> 3,
      '4' -> 4,
      '5' -> 5,
      '6' -> 6,
      '7' -> 7,
      '8' -> 8,
      '9' -> 9,
      'a' -> 10,
      'A' -> 10,
      'b' -> 11,
      'B' -> 11,
      'c' -> 12,
      'C' -> 12,
      'd' -> 13,
      'D' -> 13,
      'e' -> 14,
      'E' -> 14,
      'f' -> 15,
      'F' -> 15
    ) foreach { case (k, v) => a(k) = v }

    a to ArraySeq
  }

  def hex(c: Char): Int = if (c < 128) HEX(c) else 0
  def unescape(s: String): String = {
    val buf = new StringBuilder
    val it = s.iterator

    def ch =
      if (it.hasNext) it.next()
      else sys.error("unescape: unexpected end of string")

    while (it.hasNext) {
      ch match {
        case '\\' =>
          buf +=
            (ch match {
              case '\\' => '\\'
              case '"'  => '"'
              case '/'  => '/'
              case 'b'  => '\b'
              case 'f'  => '\f'
              case 'n'  => '\n'
              case 'r'  => '\r'
              case 't'  => '\t'
              case 'u'  => (hex(ch) << 12 | hex(ch) << 8 | hex(ch) << 4 | hex(ch)).toChar
              case c    => sys.error(s"unescape: non-escapable character: '$c' (${c.toInt})")
            })
        case c => buf += c
      }
    }

    buf.toString
  }

}
