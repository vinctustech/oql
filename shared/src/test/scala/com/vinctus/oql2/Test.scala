package com.vinctus.oql2

trait Test {

  val db: OQL

  def test(oql: String, parameters: Map[String, Any] = Map()): String = JSON(db.queryMany(oql, parameters), format = true)

}
