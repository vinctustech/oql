package com.vinctus.oql2

import typings.pg.mod.{Pool, PoolClient, PoolConfig, QueryArrayConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

class PGNodeConnection(val dataSource: PG_NodePG) extends OQLConnection {

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

  def query(sql: String): Future[PGNodeResultSet] =
    pool
      .connect()
      .toFuture
      .flatMap(
        (client: PoolClient) =>
          client
            .query[js.Array[js.Any], js.Any](QueryArrayConfig[js.Any](sql))
            .toFuture
            .map(rs => new PGNodeResultSet(rs))
            .andThen(_ => client.release()))

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  def close(): Unit = pool.end()

}
