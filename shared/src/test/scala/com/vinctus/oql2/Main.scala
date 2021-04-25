package com.vinctus.oql2

object Main extends App with OrdersDB {

  println(db.ds.schema(db.model) mkString "\n\n")
  db.showQuery()
  println(
    test("order { sum(ord_amount) count(ord_amount) agent { agent_name } } [ord_amount BETWEEN 3000 AND 4000] /agent.agent_name/ <agent.agent_name>"))

}
