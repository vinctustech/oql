package com.vinctus.oql

import scala.util.parsing.input.Position

case class Ident(s: String, pos: Position = null)

//case class Ident(s: String, pos: Position = null) {
//  def this(s: String, line: Int, col: Int) = this(s, Position(line, col))
//}
//
//case class Position(line: Int, col: Int)
