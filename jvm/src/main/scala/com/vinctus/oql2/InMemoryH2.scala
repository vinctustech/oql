package com.vinctus.oql2

class InMemoryH2 extends DataSource("H2 (in memory)") {

  def connect: Connection =
    new Connection {

      val dataSource: DataSource = InMemoryH2.this

    }

}
