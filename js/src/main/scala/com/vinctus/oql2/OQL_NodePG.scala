package com.vinctus.oql2

import typings.node.tlsMod.ConnectionOptions

import scala.scalajs.js.|

class OQL_NodePG(dm: String,
                 host: String,
                 port: Int,
                 database: String,
                 user: String,
                 password: String,
                 ssl: Boolean | ConnectionOptions,
                 idleTimeoutMillis: Int,
                 max: Int)
    extends OQL(dm, new PG_NodePG(host, port, database, user, password, ssl, idleTimeoutMillis, max), JSConversions) {}
