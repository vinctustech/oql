//package com.vinctus.oql
//
//import com.vinctus.sjs_utils.Mappable
//import typings.node.global.console
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.scalajs.js
//import js.Dynamic.{global => g}
//import scala.util.{Failure, Success}
//
//object Main extends App {
//
//  g.require("source-map-support").install()
//
//  val dm: String =
//    """
//      |entity e {
//      | *id: int
//      |  s: text
//      |}
//      |""".stripMargin
//  val data =
//    """
//      |e
//      | id: integer, pk, auto   s: text
//      |""".stripMargin
//  val db = new OQL_RDB(dm, data)
//
//  db.showQuery()
//
//  db.entity("t").insert(Map("s" -> "this is\na test")) onComplete {
//    case Success(value) =>
//      println(value)
//
//      val id = value.id.get
//
//      db.queryMany("t") onComplete {
//        case Success(value) =>
//          println(value)
//
//          db.entity("t").update(id, Map("s" -> "this is\nanother test")) onComplete {
//            case Success(value) =>
//              println(value)
//
//              db.queryMany("t") onComplete {
//                case Success(value)     => println(value)
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
//
//// todo: RDB insert without primary key value doesn't work
//// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
//// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
//// todo: unit test: "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens"
//// todo: unit test: "book.dm { author ref: &author }" (reference)

package com.vinctus.oql

import com.vinctus.sjs_utils.Mappable
import typings.node.global.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import js.Dynamic.{global => g}
import scala.util.{Failure, Success}

object Main extends App {

  g.require("source-map-support").install()

  val dm: String =
    """
      |entity t {
      | *id: int
      |  s: text
      |}
      |""".stripMargin
  val db = new OQL_NodePG_JS(dm, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  db.showQuery()

  db.entity("t").jsInsert(js.Dictionary("s" -> "this is\na test")).toFuture onComplete {
    case Success(value) =>
      console.log(value)

      val id = value.asInstanceOf[js.Dictionary[Int]]("id")

      db.jsqueryMany("t").toFuture onComplete {
        case Success(value) =>
          console.log(value)

          db.entity("t").jsUpdate(id, js.Dictionary("s" -> "this is\nanother test")).toFuture onComplete {
            case Success(value) =>
              console.log(value)

              db.jsqueryMany("t").toFuture onComplete {
                case Success(value)     => console.log(value)
                case Failure(exception) => exception.printStackTrace()
              }
            case Failure(exception) => exception.printStackTrace()
          }
        case Failure(exception) => exception.printStackTrace()
      }
    case Failure(exception) => exception.printStackTrace()
  }

}

// todo: RDB insert without primary key value doesn't work
// todo: error check for query type projects that are really datatype attributes: make sure there's no select, order, ...
// todo: add a unit test that has a deep (more than one) many-to-one reference in the select condition
// todo: unit test: "author { name } [EXISTS (books [author.name = 'Charles Dickens'])]" ~~> "Charles Dickens"
// todo: unit test: "book.dm { author ref: &author }" (reference)
