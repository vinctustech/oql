package com.vinctus.oql2

object Main extends App with OrdersDB {

  println(db.ds.schema(db.model) mkString "\n\n")
  db.showQuery()
  println(test("agent { * -phone_no orders { sum(ord_amount) } } [working_area = 'Bangalore'] <agent_code>"))

}
