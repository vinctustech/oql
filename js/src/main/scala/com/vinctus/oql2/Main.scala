package com.vinctus.oql2

import typings.node.global.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.never.onComplete
import scala.scalajs.js
import scala.util.Success

object Main extends App {

//  val dm: String =
//    """
//      |entity book {
//      | *id: bigint
//      |  title: text
//      |  year: int
//      |  author: author
//      |}
//      |
//      |entity author {
//      | *id: bigint
//      |  name: text
//      |  books: [book]
//      |}
//      |""".stripMargin
//  val db = new OQL(dm, new PG_NodePG("localhost", 5432, "postgres", "postgres", "docker", false, 0, 10))
////  val db = new OQL_NodePG(dm, "localhost", 5432, "postgres", "postgres", "docker", false, 0, 10)
//
//  db.showQuery()
//  db.json("author { name } [EXISTS (books [year < 1840])]").onComplete {
//    case Success(value) => println(value)
//  }

}
