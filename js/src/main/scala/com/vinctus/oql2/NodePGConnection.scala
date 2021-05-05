package com.vinctus.oql2

import typings.pg.mod.{Pool, PoolClient, PoolConfig, QueryArrayConfig}
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class NodePGConnection(val dataSource: PG_NodePG) extends OQLConnection {

  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s)

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

  def query(sql: String): Future[NodePGResultSet] =
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
