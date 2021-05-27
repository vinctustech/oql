package com.vinctus.oql2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

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
  val db = new OQL_NodePG(dm, "localhost", 5432, "postgres", "postgres", "postgres", false, 0, 10)

  db.showQuery()
  db.json("author { name books } [name = 'Mark Twain']") onComplete {
    case Success(json)      => println(json)
    case Failure(exception) => println(exception.getMessage)
  }

}

// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
// todo: unit test: "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens"
// todo: unit test: "book { author ref: &author }" (reference)
