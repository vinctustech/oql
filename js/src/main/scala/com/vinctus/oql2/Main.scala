package com.vinctus.oql2

import typings.node.global.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.never.onComplete
import scala.scalajs.js
import scala.util.Success

object Main extends App {

  val dm: String =
    """
      |entity book {
      | *id (pk_book_id): bigint
      |  title: text
      |  year: int
      |  author (author_id): author
      |}
      |
      |entity author {
      | *id (pk_author_id): bigint
      |  name: text
      |  books: [book]
      |}
      |""".stripMargin
////  val db = new OQL(dm, new PG_NodePG("localhost", 5432, "postgres", "postgres", "docker", false, 0, 10))
  val db = new OQL_NodePG(dm, "localhost", 5432, "postgres", "postgres", "docker", false, 0, 10)

  db.showQuery()
//  db.jsqueryMany("author { name } [id IN :ids]", js.Dictionary("ids" -> js.Array(1, 2))).toFuture.onComplete {
  db.jsqueryBuilder().query("author").getCount().toFuture.onComplete {
    case Success(value) => println(value)
  }

}

// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
// todo: add "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens" to unit tests
