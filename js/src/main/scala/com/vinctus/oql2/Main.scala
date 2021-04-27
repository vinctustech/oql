package com.vinctus.oql2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Main extends App {

  val dm: String =
    """
      |entity book {
      | *id: bigint
      |  title: text
      |  year: int
      |  author: author
      |}
      |
      |entity author {
      | *id: bigint
      |  name: text
      |  books: [book]
      |}
      |""".stripMargin
  val db = new OQL(dm, new PG_Node("localhost", 5432, "postgres", "postgres", "docker", false, 0, 10))

  db.showQuery()
  db.json("author { name } ['Oliver Twist' IN (books { title })]") onComplete {
    case Success(value) => println(value)
  }

}
