package com.vinctus.oql

import com.vinctus.sjs_utils.Mappable
import typings.node.global.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import js.Dynamic.{global => g}
import scala.util.{Failure, Success}

object Main extends App {

  g.require("source-map-support").install()

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
  val data =
    """
      |author
      | pk_author_id: integer, pk   name: text
      | 1                           Robert Louis Stevenson
      | 2                           Lewis Carroll
      | 3                           Charles Dickens
      | 4                           Mark Twain
      |
      |book
      | pk_book_id: integer, pk   title: text                         year: integer   author_id: integer, fk, author, pk_author_id
      | 1                         Treasure Island                     1883            1
      | 2                         Alice''s Adventures in Wonderland   1865            2
      | 3                         Oliver Twist                        1838            3
      | 4                         A Tale of Two Cities                1859            3
      | 5                         The Adventures of Tom Sawyer        1876            4
      | 6                         Adventures of Huckleberry Finn      1884            4
      |""".stripMargin
  val db = new OQL_RDB(dm, data)

  case class Author(id: Int, name: String)
  case class Book(id: Int, title: String, year: Int)

  db.showQuery()

  db.json("book [year = 1884]") onComplete {
    case Success(value)     => println(value)
    case Failure(exception) => println(exception)
  }

}

// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
// todo: unit test: "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens"
// todo: unit test: "book { author ref: &author }" (reference)
