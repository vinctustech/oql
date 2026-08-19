package com.vinctus.oql

import com.vinctus.oql.facades.pg.{Pool, PoolClient, PoolConfig, QueryArrayConfig}

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class NodePGConnection private (val dataSource: NodePGDataSource, pool: Pool, pinned: PoolClient)(implicit
    ec: scala.concurrent.ExecutionContext
) extends OQLConnection {

  def this(dataSource: NodePGDataSource)(implicit ec: scala.concurrent.ExecutionContext) =
    this(
      dataSource,
      new Pool(
        PoolConfig()
          .setHost(dataSource.host)
          .setPort(dataSource.port)
          .setDatabase(dataSource.database)
          .setUser(dataSource.user)
          .setPassword(dataSource.password)
          .setSsl(dataSource.ssl)
          .setIdleTimeoutMillis(dataSource.idleTimeoutMillis)
          .setMax(dataSource.max)
      ),
      null
    )

  // A pinned connection borrows one client for its whole lifetime so that every
  // statement lands inside the same transaction; an unpinned one checks a client
  // out of the pool per statement and returns it when the statement completes.
  private def withClient[T](action: PoolClient => Future[T]): Future[T] =
    if (pinned ne null) action(pinned)
    else
      pool
        .connect()
        .toFuture
        .flatMap((client: PoolClient) => action(client).andThen(_ => client.release()))

  private def statement(client: PoolClient, sql: String): Future[Unit] =
    client.query[js.Array[js.Any], js.Any](QueryArrayConfig[js.Any](sql)).toFuture.map(_ => ())

  def command(sql: String): Future[NodePGResultSet] =
    withClient(
      _.query[js.Array[js.Any], js.Any](QueryArrayConfig[js.Any](sql)).toFuture.map(rs => new NodePGResultSet(rs))
    )

  def raw(sql: String, values: js.Array[js.Any]): js.Promise[js.Array[js.Any]] =
    withClient(_.query[js.Any, js.Any](sql, values).toFuture.map(_.rows)).toJSPromise

  // Runs `body` against a connection pinned to one client, wrapped in
  // BEGIN/COMMIT, rolling back if `body` fails. A connection that is already
  // pinned joins the transaction it is in rather than nesting a new one, so a
  // failure anywhere still rolls the whole outermost transaction back.
  def transaction[R](body: NodePGConnection => Future[R]): Future[R] =
    if (pinned ne null) body(this)
    else
      pool
        .connect()
        .toFuture
        .flatMap { (client: PoolClient) =>
          val result =
            for {
              _ <- statement(client, "BEGIN")
              value <- body(new NodePGConnection(dataSource, pool, client))
              _ <- statement(client, "COMMIT")
            } yield value

          result
            .transformWith {
              case Success(value)     => Future.successful(value)
              case Failure(exception) => statement(client, "ROLLBACK").transformWith(_ => Future.failed(exception))
            }
            .andThen(_ => client.release())
        }

  def insert(command: String): Future[OQLResultSet] = ???

  def execute(command: String): Future[Unit] = ???

  def create(model: DataModel): Future[Unit] = ???

  // Ending the pool is the owning connection's job; a pinned one is released
  // when its transaction finishes.
  def close(): Unit = if (pinned eq null) pool.end()

}
