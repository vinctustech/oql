package com.vinctus.oql2

trait DBTest {

  val db: OQL

  def test(oql: String): String = JSON(db.queryMany(oql), format = true)

}
