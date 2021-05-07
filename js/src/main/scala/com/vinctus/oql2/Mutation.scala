package com.vinctus.oql2

import com.vinctus.oql2.OQL_NodePG.render
import typings.pg.pgStrings.row
import typings.std.stdStrings.map

import scala.scalajs.js
import scala.Predef.->
import scala.concurrent.ExecutionContext.Implicits.global

class Mutation(oql: OQL_NodePG, entity: Entity) {

  def insert(obj: js.Dictionary[js.Any]):  = {
    // check if the object has a primary key
    entity.pk foreach { pk =>
      // object being inserted should not have a primary key property
      if (obj.contains(pk) && obj(pk) != js.undefined)
        sys.error(s"insert(): object has a primary key property: $pk = ${obj(pk)}")
    }

    // get sub-map of all column attributes
    val attrs =
      entity.attributes
        .filter {
          case (_, _: EntityColumnAttribute) => true
          case _                             => false
        }
        .asInstanceOf[ListMap[String, EntityColumnAttribute]]

    // get sub-map of column attributes excluding primary key
    val attrsNoPK = entity.pk.fold(attrs)(attrs - _)

    // get key set of all column attributes that are required
    val attrsRequired =
      attrsNoPK.filter {
        case (_, attr: EntityColumnAttribute) => attr.required
        case _                                => false
      } keySet

    // get object's key set
    val keyset = obj.keySet

    // get key set of all attributes
    val allKeys = entity.attributes.keySet

    // check if object contains undefined attributes
    if ((keyset diff allKeys).nonEmpty)
      sys.error(s"insert(): found properties not defined for entity '$name': ${(keyset diff allKeys) map (p => s"'$p'") mkString ", "}")

    // check if object contains all required column attribute properties
    if (!(attrsRequired subsetOf keyset))
      sys.error(s"insert(): missing required properties for entity '$name': ${(attrsRequired diff keyset) map (p => s"'$p'") mkString ", "}")

    val command = new StringBuilder

    // build list of values to insert
    val pairs =
      attrsNoPK flatMap {
        case (k, _: PrimitiveEntityAttribute) if obj contains k => List(k -> render(obj(k)))
        case (k, ObjectEntityAttribute(_, typ, entity, _)) if obj contains k =>
          entity.pk match {
            case None => sys.error(s"entity '$typ' has no declared primary key")
            case Some(pk) =>
              val v = obj(k)

              List(k -> render(if (jsObject(v)) v.asInstanceOf[Map[String, Any]](pk) else v))
          }
        case (k, _) => if (attrsRequired(k)) sys.error(s"attribute '$k' is required") else Nil
      }

    // check for empty insert
    if (pairs.isEmpty)
      sys.error("empty insert")

    val (keys, values) = pairs.unzip

    // transform list of keys into un-aliased column names
    val columns = keys map (k => attrs(k).column)

    // build insert command
    command append s"INSERT INTO ${entity.table} (${columns mkString ", "}) VALUES\n"
    command append s"  (${values mkString ", "})\n"

    entity.pkcolumn foreach (pk => command append s"  RETURNING $pk\n")

      oql.show(command.toString)

    // execute insert command (to get a future)
    oql.connect.command(command.toString) map {rs =>
    if (!rs.next)
      sys.error("insert: empty result set")

      new DynamicMap(entity.pk match {
        case None => obj to ListMap
        case Some(pk) =>
          val res = obj + (pk -> row
            .next()
            .apply(0)) // only one value is being requested: the primary key

          attrs map { case (k, _) => k -> res.getOrElse(k, null) } to ListMap
      })
    }
  }

}
