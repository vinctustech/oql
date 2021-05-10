package com.vinctus.oql2

import com.vinctus.oql2.OQL_NodePG.jsObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExport

class Mutation private[oql2] (oql: OQL_NodePG, entity: Entity) {

  @JSExport
  def insert(obj: js.Dictionary[js.Any]): js.Promise[js.Dictionary[js.Any]] = {
    // check if the object has a primary key
    entity.pk foreach { pk =>
      // object being inserted should not have a primary key property
      if (obj.contains(pk.name) && obj(pk.name).asInstanceOf[js.UndefOr[_]] != js.undefined)
        sys.error(s"insert(): object has a primary key property: $pk = ${obj(pk.name)}")
    }

    // get sub-map of all column attributes
    val attrs =
      entity.attributes
        .filter {
          case (_, Attribute(name, column, pk, required, typ)) if typ.isColumnType => true
          case _                                                                   => false
        }

    // get sub-map of column attributes excluding primary key
    val attrsNoPK = entity.pk.fold(attrs)(attrs - _.name)

    // get key set of all column attributes that are required
    val attrsRequired =
      attrsNoPK.filter {
        case (_, Attribute(name, column, pk, true, typ)) if typ.isColumnType => true
        case _                                                               => false
      } keySet

    // get object's key set
    val keyset = obj.keySet

    // get key set of all attributes
    val allKeys = entity.attributes.keySet

    // check if object contains undefined attributes
    if ((keyset diff allKeys).nonEmpty)
      sys.error(s"insert(): found properties not defined for entity '${entity.name}': ${(keyset diff allKeys) map (p => s"'$p'") mkString ", "}")

    // check if object contains all required column attribute properties
    if (!(attrsRequired subsetOf keyset))
      sys.error(s"insert(): missing required properties for entity '${entity.name}': ${(attrsRequired diff keyset) map (p => s"'$p'") mkString ", "}")

    val command = new StringBuilder

    // build list of values to insert
    val pairs =
      attrsNoPK flatMap {
        case (k, Attribute(name, column, pk, required, typ)) if typ.isDataType && obj.contains(k) => List(k -> oql.render(obj(k)))
        case (k, Attribute(_, _, _, _, ManyToOneType(mtoEntity))) if obj contains k =>
          val mtopk = mtoEntity.pk.get
          val v = obj(k)

          List(k -> oql.render(if (jsObject(v)) v.asInstanceOf[js.Dictionary[Any]](mtopk.name) else v))
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
    command append s"  RETURNING ${entity.pk.get.column}\n"
    oql.show(command.toString)

    // execute insert command (to get a future)
    oql.connect.command(command.toString) map { rs =>
      if (!rs.next)
        sys.error("insert: empty result set")

      entity.pk match {
        case None     => obj
        case Some(pk) => obj(pk.name) = rs.get(0).asInstanceOf[js.Any] // only one value is being requested: the primary key
      }

      obj
    } toJSPromise
  }

  @JSExport("delete")
  def jsDelete(e: js.Any): js.Promise[Unit] = delete(if (jsObject(e)) e.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e)

  def delete(id: Any): js.Promise[Unit] = {
    val command = new StringBuilder

    // build delete command
    command append s"DELETE FROM ${entity.table}\n"
    command append s"  WHERE ${entity.pk.get.column} = ${oql.render(id)}\n"
    oql.show(command.toString)

    // execute update command (to get a future)
    oql.connect.command(command.toString) map (_ => ()) toJSPromise
  }

//  @JSExport("link")
//  def jsLink(e1: js.Any, resource: String, e2: js.Any): js.Promise[Unit] = {
//    val id1 = if (jsObject(e1)) e1.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e1
//    val id2 = if (jsObject(e2)) e2.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e2
//
//    link(id1, resource, id2)
//  }
//
//  def link(id1: Any, attribute: String, id2: Any): js.Promise[Unit] =
//    entity.attributes get attribute match {
//      case Some(Attribute(name, column, pk, required, ManyToManyType(mtmEntity, link, self, target))) =>
////      case Some(ObjectArrayJunctionEntityAttribute(_, otherEntity, attrEntityAttr, junctionType, junction)) =>
//        val thisAttr =
//          junction.attributes
//            .find {
//              case (_, attr) =>
//                attr.isInstanceOf[ObjectEntityAttribute] && attr.asInstanceOf[ObjectEntityAttribute].entity == entity
//            }
//            .get
//            ._1
//        val thatAttr =
//          junction.attributes
//            .find {
//              case (_, attr) =>
//                attr
//                  .isInstanceOf[ObjectEntityAttribute] && attr.asInstanceOf[ObjectEntityAttribute].entity == otherEntity
//            }
//            .get
//            ._1
//
//        oql.entity(junctionType).insert((thisAttr -> id1, thatAttr -> id2)) map (_ => ())
//      case Some(_) => sys.error(s"attribute '$attribute' is not many-to-many")
//      case None    => sys.error(s"attribute '$attribute' does not exist on entity '${entity.name}'")
//    }

}
