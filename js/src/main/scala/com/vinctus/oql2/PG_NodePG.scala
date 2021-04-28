package com.vinctus.oql2

import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js.|
import scalajs.js

class PG_NodePG(val host: String,
                val port: Int,
                val database: String,
                val user: String,
                val password: String,
                val ssl: Boolean | ConnectionOptions,
                val idleTimeoutMillis: Int,
                val max: Int)
    extends PGDataSource {

  val name: String = "PostgreSQL (node-pg)"

  def timestamp(t: String): Any = new js.Date(t)

  def uuid(id: String): Any = id

  val connect: NodePGConnection = new NodePGConnection(this)

  val platformSpecific: PartialFunction[Any, String] = {
    case d: js.Date => s""""${d.toISOString()}""""
  }

}
