package com.vinctus.oql

import typings.node.global.console
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import js.Dynamic.global as g
import scala.util.{Failure, Success}

import io.github.edadma.rdb

object Main extends App {

  def readFile(f: String) = g.require("fs").readFileSync(f).toString

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  val db = new OQL_RDB_ScalaJS(readFile("test/orders.dm"))

  rdb.executeSQL(
    """
      |CREATE TABLE "agent" (
      |  "agent_code" TEXT PRIMARY KEY,
      |  "agent_name" TEXT,
      |  "working_area" TEXT,
      |  "commission" NUMERIC(15, 2),
      |  "phone_no" TEXT
      |);
      |CREATE TABLE "customer" (
      |  "cust_code" TEXT PRIMARY KEY,
      |  "name" TEXT
      |);
      |CREATE TABLE "order" (
      |  "ord_num" INTEGER PRIMARY KEY,
      |  "ord_amount" NUMERIC(15, 2),
      |  "advance_amount" NUMERIC(15, 2),
      |  "ord_date" TEXT,
      |  "cust_code" TEXT,
      |  "agent_code" TEXT
      |);
      |ALTER TABLE "order" ADD FOREIGN KEY ("cust_code") REFERENCES "customer";
      |ALTER TABLE "order" ADD FOREIGN KEY ("agent_code") REFERENCES "agent";
      |INSERT INTO "agent" ("agent_code", "agent_name", "working_area", "commission", "phone_no") VALUES
      |  ('A007', 'Ramasundar', 'Bangalore', 0.15, '077-25814763'),
      |  ('A003', 'Alex', 'London', 0.13, '075-12458969'),
      |  ('A008', 'Alford', 'New York', 0.12, '044-25874365'),
      |  ('A011', 'Ravi Kumar', 'Bangalore', 0.15, '077-45625874'),
      |  ('A010', 'Santakumar', 'Chennai', 0.14, '007-22388644'),
      |  ('A012', 'Lucida', 'San Jose', 0.12, '044-52981425'),
      |  ('A005', 'Anderson', 'Brisban', 0.13, '045-21447739'),
      |  ('A001', 'Subbarao', 'Bangalore', 0.14, '077-12346674'),
      |  ('A002', 'Mukesh', 'Mumbai', 0.11, '029-12358964'),
      |  ('A006', 'McDen', 'London', 0.15, '078-22255588'),
      |  ('A004', 'Ivan', 'Torento', 0.15, '008-22544166'),
      |  ('A009', 'Benjamin', 'Hampshair', 0.11, '008-22536178');
      |INSERT INTO "customer" ("cust_code", "name") VALUES
      |  ('C00002', 'C00002 asdf'),
      |  ('C00003', 'C00003 asdf'),
      |  ('C00023', 'C00023 asdf'),
      |  ('C00007', 'C00007 asdf'),
      |  ('C00008', 'C00008 asdf'),
      |  ('C00025', 'C00025 asdf'),
      |  ('C00004', 'C00004 asdf'),
      |  ('C00021', 'C00021 asdf'),
      |  ('C00011', 'C00011 asdf'),
      |  ('C00001', 'C00001 asdf'),
      |  ('C00020', 'C00020 asdf'),
      |  ('C00006', 'C00006 asdf'),
      |  ('C00005', 'C00005 asdf'),
      |  ('C00018', 'C00018 asdf'),
      |  ('C00014', 'C00014 asdf'),
      |  ('C00022', 'C00022 asdf'),
      |  ('C00009', 'C00009 asdf'),
      |  ('C00010', 'C00010 asdf'),
      |  ('C00017', 'C00017 asdf'),
      |  ('C00024', 'C00024 asdf'),
      |  ('C00015', 'C00015 asdf'),
      |  ('C00012', 'C00012 asdf'),
      |  ('C00019', 'C00019 asdf'),
      |  ('C00016', 'C00016 asdf');
      |INSERT INTO "order" ("ord_num", "ord_amount", "advance_amount", "ord_date", "cust_code", "agent_code") VALUES
      |  (200114, 3500, 2000, '15-AUG-08', 'C00002', 'A008'),
      |  (200122, 2500, 400, '16-SEP-08', 'C00003', 'A004'),
      |  (200118, 500, 100, '20-JUL-08', 'C00023', 'A006'),
      |  (200119, 4000, 700, '16-SEP-08', 'C00007', 'A010'),
      |  (200121, 1500, 600, '23-SEP-08', 'C00008', 'A004'),
      |  (200130, 2500, 400, '30-JUL-08', 'C00025', 'A011'),
      |  (200134, 4200, 1800, '25-SEP-08', 'C00004', 'A005'),
      |  (200108, 4000, 600, '15-FEB-08', 'C00008', 'A004'),
      |  (200103, 1500, 700, '15-MAY-08', 'C00021', 'A005'),
      |  (200105, 2500, 500, '18-JUL-08', 'C00025', 'A011'),
      |  (200109, 3500, 800, '30-JUL-08', 'C00011', 'A010'),
      |  (200101, 3000, 1000, '15-JUL-08', 'C00001', 'A008'),
      |  (200111, 1000, 300, '10-JUL-08', 'C00020', 'A008'),
      |  (200104, 1500, 500, '13-MAR-08', 'C00006', 'A004'),
      |  (200106, 2500, 700, '20-APR-08', 'C00005', 'A002'),
      |  (200125, 2000, 600, '10-OCT-08', 'C00018', 'A005'),
      |  (200117, 800, 200, '20-OCT-08', 'C00014', 'A001'),
      |  (200123, 500, 100, '16-SEP-08', 'C00022', 'A002'),
      |  (200120, 500, 100, '20-JUL-08', 'C00009', 'A002'),
      |  (200116, 500, 100, '13-JUL-08', 'C00010', 'A009'),
      |  (200124, 500, 100, '20-JUN-08', 'C00017', 'A007'),
      |  (200126, 500, 100, '24-JUN-08', 'C00022', 'A002'),
      |  (200129, 2500, 500, '20-JUL-08', 'C00024', 'A006'),
      |  (200127, 2500, 400, '20-JUL-08', 'C00015', 'A003'),
      |  (200128, 3500, 1500, '20-JUL-08', 'C00009', 'A002'),
      |  (200135, 2000, 800, '16-SEP-08', 'C00007', 'A010'),
      |  (200131, 900, 150, '26-AUG-08', 'C00012', 'A012'),
      |  (200133, 1200, 400, '29-JUN-08', 'C00009', 'A002'),
      |  (200100, 1000, 600, '08-JAN-08', 'C00015', 'A003'),
      |  (200110, 3000, 500, '15-APR-08', 'C00019', 'A010'),
      |  (200107, 4500, 900, '30-AUG-08', 'C00007', 'A010'),
      |  (200112, 2000, 400, '30-MAY-08', 'C00016', 'A007'),
      |  (200113, 4000, 600, '10-JUN-08', 'C00022', 'A002'),
      |  (200102, 2000, 300, '25-MAY-08', 'C00012', 'A012');
      |""".stripMargin
  )(
    db.connect
      .asInstanceOf[RDBConnection]
      .db
  )

  db.showQuery()
  db.queryMany("agent { * -phone_no orders { sum(ord_amount) } } [working_area = 'Bangalore'] <agent_code>")
    .onComplete {
      case Failure(exception) => exception.printStackTrace()
      case Success(value)     => println(value)
    }

//    new OQL_NodePG_ScalaJS(g.require("fs").readFileSync("test/json.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
  // new OQL_NodePG_JS(g.require("fs").readFileSync("test/json.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
//    new OQL_NodePG_ScalaJS(g.require("fs").readFileSync("test/book.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
//    new OQL_NodePG_JS(g.require("fs").readFileSync("test/event.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
//  new OQL_NodePG(g.require("fs").readFileSync("test/accounts.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
//    println(await(db.entity("info").insert(Map("main" -> 1, "data" -> List(1, Map("asdf" -> 345), 3)))))
//    println(await(db.entity("info").update(3, Map("data" -> List(5, 6, 7)))))
//      db.showQuery()
//    println(await(db.queryMany("main {* infos}")))
//    println(stringify(await(db.jsQueryMany("main {* infos}").toFuture)))
//    println(await(db.queryMany("info")))
//    println(stringify(await(db.jsQueryMany("info").toFuture)))
//    println(await(db.queryMany("job { jobTitle employees { firstName } }")))
//    println(await(db.queryMany("author {* books: books {count(*)}}")))
  // println(await(db.queryMany("book {* author: author.name} <id>")))
//    println(await(db.json("car")))
//    println(await(db.queryMany("""account""", "account", 2)))
//    println(await(db.queryMany("""vehicle""", "account", 2)))
//    println(await(db.queryMany("""store""", "account", 2)))
//    println(await(db.jsQueryMany("attendee { * events <when> } <name>").toFuture map (v => JSON(v, db.ds.platformSpecific, 2, true))))

  def stringify(x: Any) = js.JSON.stringify(x.asInstanceOf[js.Any], null.asInstanceOf[js.Array[js.Any]], 2)

}

//package com.vinctus.oql
//
//import com.vinctus.sjs_utils.Mappable
//import typings.node.global.console
//
//import *$1*
//import scala.async.Async.{async, await}
//
//import scala.scalajs.js
//import js.Dynamic.{global => g}
//
//object Main extends App {
//
//  g.require("source-map-support").install()
//
//  val dm: String =
//    """
//      |entity t {
//      | *id: int
//      |  s: text
//      |}
//      |""".stripMargin
//  val data =
//    """
//      |t
//      | id: integer, pk, auto   s: text
//      |""".stripMargin
//  val db = new OQL_RDB(dm, data)
//
//  async {
//    db.showQuery()
//    await(db.entity("t").insert(Map("s" -> "asdf'zxcv")))
//    db.showQuery()
//    println(await(db.queryMany("""t""")))
//  }
//
//}

//package com.vinctus.oql
//
//import com.vinctus.sjs_utils.Mappable
//import typings.node.global.console
//
//import *$1*
//import scala.scalajs.js
//import js.Dynamic.{global => g}
//import scala.util.{Failure, Success}
//import scala.async.Async.{async, await}
//
//object Main extends App {
//
//  g.require("source-map-support").install()
//
//  val dm: String =
//    """
//        |entity t {
//        | *id: int
//        |  n: int
//        |}
//        |""".stripMargin
//  val db = new OQL_NodePG_JS(dm, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
//
//  db.showQuery()
//
//  async {
//    console.log(await(db.jsQueryMany("""t { average: avg(n) }""").toFuture))
//  }
//
//}

//  val dm: String =
//    """
//      |entity t {
//      | *id: int
//      |  s: text
//      |}
//      |""".stripMargin
//  val db = new OQL_NodePG_JS(dm, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)
//
//  db.showQuery()
//  db.entity("t").jsInsert(js.Dictionary("s" -> "asdf'zxcv")).toFuture onComplete {
//    case Success(value) =>
//      console.log(value)
//
//      val id = value.asInstanceOf[js.Dictionary[Int]]("id")
//
//      db.jsqueryMany("t").toFuture onComplete {
//        case Success(value) =>
//          console.log(value)
//
//          db.showQuery()
//          db.entity("t").jsUpdate(id, js.Dictionary("s" -> "this is\nanother test")).toFuture onComplete {
//            case Success(value) =>
//              console.log(value)
//
//              db.jsqueryMany("t").toFuture onComplete {
//                case Success(value)     => console.log(value)
//                case Failure(exception) => exception.printStackTrace()
//              }
//            case Failure(exception) => exception.printStackTrace()
//          }
//        case Failure(exception) => exception.printStackTrace()
//      }
//    case Failure(exception) => exception.printStackTrace()
//  }
//
//}

// todo: RDB insert without primary key value doesn't work
// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
// todo: unit test: "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens"
// todo: unit test: "book.dm { author ref: &author }" (reference)

/*

HashMap(
  vehicle ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(driver,null), Ident(account,null), Ident(id,null)),List((Entity(user,user),Attribute(driver,driver,false,false,ManyToOneType(Entity(user,user)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))), AttributeOQLExpression(List(Ident(store,null), Ident(account,null), Ident(id,null)),List((Entity(store,store),Attribute(store,store,false,true,ManyToOneType(Entity(store,store)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))))),
  trip ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(customer,null), Ident(store,null), Ident(account,null), Ident(id,null)),List((Entity(customer,customer),Attribute(customer,customer,false,false,ManyToOneType(Entity(customer,customer)))), (Entity(store,store),Attribute(store,store,false,true,ManyToOneType(Entity(store,store)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))),
        AttributeOQLExpression(List(Ident(store,null), Ident(account,null), Ident(id,null)),List((Entity(store,store),Attribute(store,store,false,true,ManyToOneType(Entity(store,store)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))), AttributeOQLExpression(List(Ident(vehicle,null), Ident(driver,null), Ident(account,null), Ident(id,null)),List((Entity(vehicle,vehicle),Attribute(vehicle,vehicle,false,false,ManyToOneType(Entity(vehicle,vehicle)))), (Entity(user,user),Attribute(driver,driver,false,false,ManyToOneType(Entity(user,user)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))),
        AttributeOQLExpression(List(Ident(vehicle,null), Ident(store,null), Ident(account,null), Ident(id,null)),List((Entity(vehicle,vehicle),Attribute(vehicle,vehicle,false,false,ManyToOneType(Entity(vehicle,vehicle)))), (Entity(store,store),Attribute(store,store,false,true,ManyToOneType(Entity(store,store)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))))),
  store ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(account,null), Ident(id,null)),List((Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))))),
  customer ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(store,null), Ident(account,null), Ident(id,null)),List((Entity(store,store),Attribute(store,store,false,true,ManyToOneType(Entity(store,store)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))))),
  users_stores ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(user,null), Ident(account,null), Ident(id,null)),List((Entity(user,user),Attribute(user,user,false,false,ManyToOneType(Entity(user,user)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))),
        AttributeOQLExpression(List(Ident(store,null), Ident(account,null), Ident(id,null)),List((Entity(store,store),Attribute(store,store,false,false,ManyToOneType(Entity(store,store)))), (Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))))),
  account ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(id,null)),List((Entity(account,account),Attribute(id,id,true,false,IntegerType)))))),
  user ->
    Map(Entity(account,account) ->
      List(
        AttributeOQLExpression(List(Ident(account,null), Ident(id,null)),List((Entity(account,account),Attribute(account,account,false,true,ManyToOneType(Entity(account,account)))), (Entity(account,account),Attribute(id,id,true,false,IntegerType)))))))

 */
