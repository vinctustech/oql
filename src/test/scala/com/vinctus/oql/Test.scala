package com.vinctus.oql

import com.vinctus.sjs_utils.fromJS

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
    dbjs.jsQueryMany(oql, parameters = parameters).toFuture map (v => JSON(v, db.ds.platformSpecific, format = true))

  def testmap(oql: String, parameters: (String, Any)*): Future[Any] = db.queryMany(oql, parameters = parameters.toMap)

  def testmapjs(oql: String, parameters: js.UndefOr[js.Any] = js.undefined): Future[Any] =
    dbjs.jsQueryMany(oql, parameters = parameters).toFuture map (r => fromJS(r))

}
