package com.vinctus.oql2

import scala.language.postfixOps

trait UNDB extends Test {

  val dm: String =
    """
      |entity country {
      | *id: bigint
      |  name: text
      |  rep: <rep>
      |}
      |
      |entity rep {
      | *id: bigint
      |  name: text
      |  country: country
      |}
      |""".stripMargin
  val db = new OQL(dm, ds)

  db.create()
  db.execute(_.insert("""insert into country (id, name) values 
                        |(1,'Nigeria'),
                        |(2,'Ghana'),
                        |(3,'South Africa'),
                        |(4,'Republic of China (Taiwan)')
                        |""".stripMargin))
  db.execute(_.insert("""insert into rep (id, name, country) values 
                        |(1,'Abubakar Ahmad', 1),
                        |(2,'Joseph Nkrumah', 2),
                        |(3,'Lauren Zuma', 3),
                        |(4,'Batman', null)
                        |""".stripMargin))

}
