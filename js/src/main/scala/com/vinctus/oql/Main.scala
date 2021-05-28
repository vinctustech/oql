package com.vinctus.oql

import com.vinctus.sjs_utils.Mappable

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

  case class Author(id: Int, name: String)
  case class Book(id: Int, title: String, year: Int)

  db.showQuery()
  db.ccQueryMany[Book]("book [author.name = 'Mark Twain']") onComplete {
    case Success(result)    => println(result)
    case Failure(exception) => println(exception.getMessage)
  }

}

// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
// todo: unit test: "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens"
// todo: unit test: "book { author ref: &author }" (reference)
