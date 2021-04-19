package com.vinctus.oql2

trait BookDB extends Test {

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
  val db = new OQL(dm, new H2mem)

  db.create()
  db.execute(_.insert("""insert into author (name) values 
                        |('Robert Louis Stevenson'),
                        |('Lewis Carroll'),
                        |('Charles Dickens'),
                        |('Mark Twain')
                        |""".stripMargin))
  db.execute(_.insert("""insert into book (title, year, author) values 
                        |('Treasure Island', 1883, 1),
                        |('Alice’s Adventures in Wonderland', 1865, 2),
                        |('Oliver Twist', 1838, 3),
                        |('A Tale of Two Cities', 1859, 3),
                        |('The Adventures of Tom Sawyer', 1876, 4),
                        |('Adventures of Huckleberry Finn', 1884, 4)
                        |""".stripMargin))

}
