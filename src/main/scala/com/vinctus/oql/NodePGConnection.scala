package com.vinctus.oql

import typings.pg.mod.{Pool, PoolClient, PoolConfig, QueryArrayConfig}

import scala.concurrent.Future

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class NodePGConnection(val dataSource: NodePG)(implicit ec: scala.concurrent.ExecutionContext) extends OQLConnection {

  private val pool = new Pool(
    PoolConfig()
      .setHost(dataSource.host)
      .setPort(dataSource.port)
      .setDatabase(dataSource.database)
      .setUser(dataSource.user)
      .setPassword(dataSource.password)
      .setSsl(dataSource.ssl)
      .setIdleTimeoutMillis(dataSource.idleTimeoutMillis)
      .setMax(dataSource.max))

  def command(sql: String): Future[NodePGResultSet] =
    pool
      .connect()
      .toFuture
      .flatMap(
        (client: PoolClient) =>
          client
            .query[js.Array[js.Any], js.Any](QueryArrayConfig[js.Any](sql))
            .toFuture
            .map(rs => new NodePGResultSet(rs))
            .andThen(_ => client.release()))

  def raw(sql: String, values: js.Array[js.Any]): js.Promise[js.Array[js.Any]] =
    pool
      .connect()
      .toFuture
      .flatMap(
        (client: PoolClient) =>
          client
            .query[js.Any, js.Any](sql, values)
            .toFuture
            .map(_.rows)
            .andThen(_ => client.release()))
      .toJSPromise

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  def close(): Unit = pool.end()

}
