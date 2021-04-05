package com.vinctus.oql2

import scala.collection.immutable.VectorMap
import scala.collection.mutable

class DataModel(model: DMLModel, dml: String) {

  val entities: Map[String, Entity] = {
    var error = false
    val map = new mutable.HashMap[String, Entity]

    def duplicates(ids: Seq[Ident], typ: String): Unit =
      ids.groupBy(_.s).toList filter { case (_, v) => v.length > 1 } flatMap { case (_, s) => s } match {
        case Nil =>
        case duplicates =>
          duplicates foreach { id =>
            printError(id.pos, s"duplicate effective $typ name: '${id.s}'", dml)
          }

          error = true
      }

    duplicates(model.entities.map(e => e.alias getOrElse e.name), "entity")

    for (entity <- model.entities) {
      duplicates(entity.attributes.map(a => a.alias getOrElse a.name), "attribute")

      entity.attributes.filter(_.pk) match {
        case as if as.length > 1 => as foreach (a => printError(a.name.pos, s"extraneous primary key", dml))
        case _                   =>
      }

      map((entity.alias getOrElse entity.name).s) = new Entity(
        (entity.alias getOrElse entity.name).s,
        entity.name.s,
        entity.attributes
          .map(a =>
            ((a.alias getOrElse a.name).s, new Attribute((a.alias getOrElse a.name).s, a.name.s, a.pk, a.required))) to VectorMap
      )
    }

    if (error)
      sys.error("errors while creating data model")

    for (DMLEntity(name, alias, attributes) <- model.entities) {}

    Map()
  }

}

class Entity(name: String, table: String, attributes: Map[String, Attribute])

class Attribute(name: String, column: String, pk: Boolean, required: Boolean)
