package com.vinctus.oql2

import org.parboiled2.{ParseError, ParserInput}

import scala.util.{Failure, Success}

object Main extends App {

  val in = ParserInput("entity asdf { }  zxcv { }")
  val p = new DMLParser(in)

  p.model.run() match {
    case Failure(ParseError(position, principalPosition, traces)) =>
      println(position /*, principalPosition, traces*/ )
    case Success(value) => println(value)
  }

}
