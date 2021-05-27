package com.vinctus.oql2

import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js.|
import scalajs.js

class NodePG(val host: String,
             val port: Int,
             val database: String,
             val user: String,
             val password: String,
             val ssl: Boolean | ConnectionOptions,
             val idleTimeoutMillis: Int,
             val max: Int)
    extends PGDataSource {

  val name: String = "PostgreSQL (node-pg)"

  // this has to be a `val` and not a `def` because it contains a `node-pg` connection pool
  // NodePGConnection should only be instantiated once
  val connect: NodePGConnection = new NodePGConnection(this)

  val platformSpecific: PartialFunction[Any, String] = {
    case d: js.Date => s""""${d.toISOString()}""""
  }

}