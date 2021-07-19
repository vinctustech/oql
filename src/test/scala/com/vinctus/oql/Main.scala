package com.vinctus.oql

import com.vinctus.sjs_utils.Mappable
import typings.node.global.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.{async, await}

import scala.scalajs.js
import js.Dynamic.{global => g}

object Main extends App {

  g.require("source-map-support").install()

  val db =
    new OQL_NodePG(g.require("fs").readFileSync("test/accounts.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  println(db.model.entities.view.mapValues(e => e.fixing).toMap)

  async {
    db.showQuery()
//    println(await(db.queryMany("""account""", "account", 2)))
    println(await(db.queryMany("""vehicle""", "account", 2)))
//    println(await(db.queryMany("""store""", "account", 2)))
  } recover {
    case e: Exception => println(e)
  }

}

//package com.vinctus.oql
//
//import com.vinctus.sjs_utils.Mappable
//import typings.node.global.console
//
//import scala.concurrent.ExecutionContext.Implicits.global
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
//import scala.concurrent.ExecutionContext.Implicits.global
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
