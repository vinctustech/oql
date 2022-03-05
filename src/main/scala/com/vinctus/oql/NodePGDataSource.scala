package com.vinctus.oql

import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js.|
import scalajs.js

class NodePGDataSource(
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
    val ssl: Boolean | ConnectionOptions,
    val idleTimeoutMillis: Int,
    val max: Int
)(implicit ec: scala.concurrent.ExecutionContext)
    extends PGDataSource {

  val name: String = "PostgreSQL (node-pg)"

  // this has to be a `val` and not a `def` because it contains an `node-pg` connection pool
  // NodePGConnection should only be instantiated once
  val connect: NodePGConnection = new NodePGConnection(this)

  val platformSpecific: PartialFunction[Any, String] = { case d: js.Date =>
    s""""${d.toISOString()}""""
  }

}
