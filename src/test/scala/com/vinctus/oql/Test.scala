package com.vinctus.oql

import scala.scalajs.js

import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.js.Dynamic.{global => g}

import scala.concurrent.ExecutionContext.Implicits.global

trait Test {

  val dm: String
  lazy val db =
    new OQL_NodePG(g.require("fs").readFileSync(s"test/$dm.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
  lazy val dbjs =
    new OQL_NodePG_JS(g.require("fs").readFileSync(s"test/$dm.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  def test(oql: String, parameters: (String, Any)*): Future[String] = db.json(oql, parameters = parameters.toMap)

  def testjs(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): Future[String] =
    dbjs.jsQueryMany(oql, parameters = parameters).toFuture map (v => js.JSON.stringify(v, null.asInstanceOf[js.Array[js.Any]], 2) :+ '\n')

}
