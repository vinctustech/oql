package com.vinctus.oql2

import com.vinctus.oql2.OQL_NodePG.jsObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExport
import scala.concurrent.Future

class Mutation private[oql2] (oql: OQL_NodePG, entity: Entity) {

  @JSExport("insert")
  def jsinsert(obj: js.Dictionary[js.Any]): js.Promise[js.Dictionary[js.Any]] = insert(obj).toJSPromise

  def insert(obj: js.Dictionary[js.Any]): Future[js.Dictionary[js.Any]] = {
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
    }
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

  @JSExport("link")
  def jsLink(e1: js.Any, resource: String, e2: js.Any): js.Promise[Unit] = {
    val id1: js.Any = if (jsObject(e1)) e1.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e1
    val id2: js.Any = if (jsObject(e2)) e2.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e2

    link(id1, resource, id2)
  }

  def link(id1: js.Any, attribute: String, id2: js.Any): js.Promise[Unit] =
    entity.attributes get attribute match {
      case Some(Attribute(name, column, pk, required, ManyToManyType(mtmEntity, link, self, target))) =>
        new Mutation(oql, link).insert(js.Dictionary(self.column -> id1, target.column -> id2)) map (_ => ()) toJSPromise
      case Some(_) => sys.error(s"attribute '$attribute' is not many-to-many")
      case None    => sys.error(s"attribute '$attribute' does not exist on entity '${entity.name}'")
    }

  @JSExport("unlink")
  def jsUnlink(e1: js.Any, resource: String, e2: js.Any): js.Promise[Unit] = {
    val id1: Any = if (jsObject(e1)) e1.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e1
    val id2: Any = if (jsObject(e2)) e2.asInstanceOf[js.Dictionary[String]](entity.pk.get.name) else e2

    unlink(id1, resource, id2).toJSPromise
  }

  def unlink(id1: Any, attribute: String, id2: Any): Future[Unit] =
    entity.attributes get attribute match {
      case Some(Attribute(name, column, pk, required, ManyToManyType(mtmEntity, link, self, target))) =>
        val command = new StringBuilder

        // build delete command
        command append s"DELETE FROM ${link.table}\n"
        command append s"  WHERE ${self.column} = ${oql.render(id1)} AND ${target.column} = ${oql.render(id2)}\n"
        oql.show(command.toString)

        // execute update command (to get a future)
        oql.connect.command(command.toString) map (_ => ())
      case Some(_) => sys.error(s"attribute '$attribute' is not many-to-many")
      case None    => sys.error(s"attribute '$attribute' does not exist on entity '${entity.name}'")
    }

  @JSExport("update")
  def jsUpdate(e: js.Any, updates: js.Any): js.Promise[Unit] =
    update(if (jsObject(e)) e.asInstanceOf[js.Dictionary[Any]](entity.pk.get.name) else e, updates.asInstanceOf[js.Dictionary[Any]]).toJSPromise

  def update(e: Any, updates: collection.Map[String, Any]): Future[Unit] = {
    // check if updates has a primary key
    entity.pk foreach (pk =>
      // object being updated should not have it's primary key changed
      if (updates.contains(pk.name))
        sys.error(s"update: primary key can not be changed: $pk"))

    // get sub-map of all column attributes
    val attrs =
      entity.attributes
        .filter {
          case (_, Attribute(name, column, pk, required, typ)) if typ.isColumnType => true
          case _                                                                   => false
        }

    // get sub-map of column attributes excluding primary key
    val attrsNoPK = entity.pk.fold(attrs)(attrs - _.name)

    // get key set of column attributes excluding primary key
    val attrsNoPKKeys = attrsNoPK.keySet

    // get updates key set
    val keyset = updates.keySet

    // check if object contains extrinsic attributes
    if ((keyset diff attrsNoPKKeys).nonEmpty)
      sys.error(s"extrinsic properties not found in entity '${entity.name}': ${(keyset diff attrsNoPKKeys) map (p => s"'$p'") mkString ", "}")

    // build list of attributes to update
    val pairs =
      updates map {
        case (k, v) =>
          val v1 =
            if (jsObject(v))
              entity.attributes(k) match {
                case Attribute(_, _, _, _, ManyToOneType(mtoEntity)) if mtoEntity.pk.isDefined =>
                  v.asInstanceOf[Map[String, Any]](mtoEntity.pk.get.name)
                case Attribute(_, _, _, _, ManyToOneType(mtoEntity)) =>
                  sys.error(s"entity '${mtoEntity.name}' does not have a declared primary key")
                case _ => sys.error(s"attribute '$k' of entity '${entity.name}' is not an entity attribute")
              } else v

          attrs(k).column -> oql.render(v1)
      }

    val command = new StringBuilder
    val id =
      e match {
        case m: Map[_, _] =>
          m.asInstanceOf[Map[String, Any]].get(entity.pk.get.name) match {
            case None     => sys.error(s"primary key not found in map: ${entity.pk.get}")
            case Some(pk) => pk
          }
        case p: Product =>
          p.productElementNames.indexOf(entity.pk.get) match {
            case -1  => sys.error(s"primary key not found in case class: ${entity.pk.get}")
            case idx => p.productElement(idx)
          }
        case _ => e
      }

    // build update command
    command append s"UPDATE ${entity.table}\n"
    command append s"  SET ${pairs map { case (k, v) => s"$k = $v" } mkString ", "}\n"
    command append s"  WHERE ${entity.pk.get.column} = ${oql.render(id)}\n"
    oql.show(command.toString)

    // execute update command (to get a future)
    oql.connect.command(command.toString) map (_ => ())
  }

}
