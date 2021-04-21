package com.vinctus.oql2

import scala.language.postfixOps
import scala.sys.process._

trait UNDBPG extends Test {

  "docker container stop pg-docker" !

  "docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres" !

  Thread.sleep(1000)

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
  val db = new OQL(dm, new H2mem)

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
