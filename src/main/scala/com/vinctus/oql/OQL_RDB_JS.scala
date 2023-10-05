package com.vinctus.oql

import typings.node.tlsMod.ConnectionOptions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.{Promise, |}
import scala.util.matching.Regex
import io.github.edadma.rdb

@JSExportTopLevel("OQL_MEM")
class OQL_RDB_JS(
    dm: String
) extends AbstractOQL(
      dm,
      new RDBDataSource,
      JSConversions
    ) {

  def execute[R](action: OQLConnection => Future[R]): Future[R] = action(connect)

  @JSExport("create")
  def jsCreate(): Promise[Unit] = create.toJSPromise

  @JSExport
  def entity(name: String): Mutation_JS_RDB = new Mutation_JS_RDB(this, model.entities(name))

  @JSExport("showQuery")
  def jsShowQuery(): Unit = showQuery()

  @JSExport("define")
  def jsDefine(name: String, definition: String, parameters: js.Array[String]): Unit =
    define(name, definition, parameters.toSeq)

  @JSExport("count")
  def jsCount(
      oql: String,
      parameters: js.UndefOr[js.Any] = js.undefined,
      fixed: js.UndefOr[String] = null,
      at: js.Any = null
  ): js.Promise[Int] =
    count(substitute(oql, parameters), fixed.orNull, at).toJSPromise

  @JSExport("queryOne")
  def jsQueryOne(
      oql: String,
      parameters: js.UndefOr[js.Any] = js.undefined,
      fixed: js.UndefOr[String] = null,
      at: js.Any = null
  ): js.Promise[js.UndefOr[Any]] = {
    val subst = substitute(oql, parameters)

    jsQueryOne(parseQuery(subst), subst, fixedEntity(fixed.orNull, at))
  }

  def jsQueryOne(query: OQLQuery, oql: String, fixed: Fixed): js.Promise[js.UndefOr[Any]] =
    jsQueryMany(query, oql, fixed).toFuture map {
      case a if a.length == 0 => js.undefined
      case a if a.length == 1 => a.head
      case _                  => sys.error(s"queryOne: more than one row was found")
    } toJSPromise

  @JSExport("queryMany")
  def jsQueryMany(
      oql: String,
      parameters: js.UndefOr[js.Any] = js.undefined,
      fixed: js.UndefOr[String] = null,
      at: js.Any = null
  ): js.Promise[js.Array[js.Any]] = {
    val subst = substitute(oql, parameters)

    jsQueryMany(parseQuery(subst), subst, fixedEntity(fixed.orNull, at))
  }

  def jsQueryMany(query: OQLQuery, oql: String, fixed: Fixed): js.Promise[js.Array[js.Any]] =
    queryMany(query, oql, () => new JSResultBuilder, fixed)
      .map(_.arrayResult.asInstanceOf[js.Array[js.Any]])
      .toJSPromise

  @JSExport("queryBuilder")
  def jsQueryBuilder(fixed: js.UndefOr[String], at: js.Any) =
    new QueryBuilder_JS_RDB(
      this,
      OQLQuery(null, null, null, List(StarOQLProject), None, None, None, None, None, None),
      fixedEntity(fixed.orNull, at)
    )

  @JSExport
  def raw(sql: String, values: js.UndefOr[js.Array[js.Any]]): js.Promise[js.Array[js.Any]] =
    ds.asInstanceOf[RDBDataSource]
      .connect
      .raw(sql, if (values.isEmpty) Vector() else values.get.toIndexedSeq.asInstanceOf[IndexedSeq[Any]])
      .map(t => t map (_.toJSArray) toJSArray)
      .asInstanceOf[Future[js.Array[js.Any]]]
      .toJSPromise

  @JSExport
  def rawMulti(sql: String) =
    ds.asInstanceOf[RDBDataSource]
      .connect
      .rawMulti(sql)
      .toJSPromise

  private val varRegex = ":([a-zA-Z_][a-zA-Z0-9_]*)" r

  def substitute(s: String, parameters: js.UndefOr[js.Any]): String = // todo: unit tests for parameters
    if (parameters.isEmpty) s
    else
      varRegex.replaceAllIn(
        s,
        m =>
          parameters.asInstanceOf[js.Dictionary[Any]] get m.group(1) match {
            case None        => sys.error(s"template: parameter '${m.group(1)}' not found")
            case Some(value) => Regex.quoteReplacement(subsrender(value))
          }
      )

  def subsrender(a: Any): String =
    a match {
      case s: String =>
        s"'${s
            .replace("\\", """\\""")
            .replace("'", """\'""")
            .replace("\r", """\r""")
            .replace("\n", """\n""")}'"
      case d: js.Date     => s"'${d.toISOString()}'"
      case a: js.Array[_] => s"(${a map subsrender mkString ","})"
      case _              => String.valueOf(a)
    }

  def render(a: Any, typ: Option[Datatype] = None): String =
    if (typ.isDefined)
      if (typ.get == JSONType) s"'${JSON(a, ds.platformSpecific)}'"
      else ds.typed(a, typ.get)
    else
      a match {
        case s: String      => ds.string(s)
        case d: js.Date     => s"'${d.toISOString()}'"
        case a: js.Array[_] => s"(${a map (e => render(e, typ)) mkString ","})"
        case _              => String.valueOf(a)
      }

}

//todo: investigate 'subsrender' thoroughly
