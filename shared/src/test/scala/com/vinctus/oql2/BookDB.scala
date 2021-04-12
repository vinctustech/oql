package com.vinctus.oql2

trait BookDB extends Test {

  val dm: String =
    """
      |entity book {
      | *id: bigint
      |  title: text
      |  year: int
      |  author: author
      |  tags: [tag]
      |}
      |
      |entity tag {
      | *id: bigint
      |  tag: text
      |  book: book
      |}
      |
      |entity article {
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
      |  articles: [article]
      |}
      |""".stripMargin
  val db = new OQL(dm, new H2_mem)

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
  db.execute(_.insert("""insert into tag (tag, book) values 
                        |('adventure', 1)
                        |""".stripMargin))
  db.execute(_.insert("""insert into article (title, year, author) values 
                        |('art1a', 1883, 1),
                        |('art1b', 1883, 1),
                        |('art2', 1865, 2),
                        |('art3', 1838, 3),
                        |('art4a', 1876, 4),
                        |('art4b', 1884, 4)
                        |""".stripMargin))

}
