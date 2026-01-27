package com.vinctus.oql.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSGlobal}

// TLS/SSL ConnectionOptions from Node.js
@js.native
trait ConnectionOptions extends js.Object

// pg-types TypeId (just a numeric type identifier)
type TypeId = Double

// pg module types
object pg {

  @JSImport("pg", "Pool")
  @js.native
  class Pool(config: PoolConfig) extends js.Object {
    def connect(): js.Promise[PoolClient] = js.native
    def end(): js.Promise[Unit] = js.native
  }

  @js.native
  trait PoolConfig extends js.Object {
    var host: js.UndefOr[String] = js.native
    var port: js.UndefOr[Int] = js.native
    var database: js.UndefOr[String] = js.native
    var user: js.UndefOr[String] = js.native
    var password: js.UndefOr[String] = js.native
    var ssl: js.UndefOr[Boolean | ConnectionOptions] = js.native
    var idleTimeoutMillis: js.UndefOr[Int] = js.native
    var max: js.UndefOr[Int] = js.native
  }

  object PoolConfig {
    def apply(): PoolConfig = js.Dynamic.literal().asInstanceOf[PoolConfig]

    extension (c: PoolConfig) {
      def setHost(v: String): PoolConfig = { c.host = v; c }
      def setPort(v: Int): PoolConfig = { c.port = v; c }
      def setDatabase(v: String): PoolConfig = { c.database = v; c }
      def setUser(v: String): PoolConfig = { c.user = v; c }
      def setPassword(v: String): PoolConfig = { c.password = v; c }
      def setSsl(v: Boolean | ConnectionOptions): PoolConfig = { c.ssl = v; c }
      def setIdleTimeoutMillis(v: Int): PoolConfig = { c.idleTimeoutMillis = v; c }
      def setMax(v: Int): PoolConfig = { c.max = v; c }
    }
  }

  @js.native
  trait PoolClient extends js.Object {
    def query[R, I](config: QueryArrayConfig[I]): js.Promise[QueryArrayResult[R]] = js.native
    def query[R, I](text: String, values: js.Array[I]): js.Promise[QueryResult[R]] = js.native
    def release(): Unit = js.native
  }

  @js.native
  trait QueryArrayConfig[I] extends js.Object {
    var text: String = js.native
    var rowMode: String = js.native
  }

  object QueryArrayConfig {
    def apply[I](text: String): QueryArrayConfig[I] = {
      val config = js.Dynamic.literal(text = text, rowMode = "array")
      config.asInstanceOf[QueryArrayConfig[I]]
    }
  }

  @js.native
  trait QueryArrayResult[R] extends js.Object {
    val rows: js.Array[R] = js.native
    val rowCount: Double | Null = js.native
  }

  @js.native
  trait QueryResult[R] extends js.Object {
    val rows: js.Array[R] = js.native
  }

  @js.native
  @JSImport("pg", "types")
  object types extends js.Object {
    def setTypeParser(typeId: TypeId, parser: js.Function1[String, Any]): Unit = js.native
  }
}
