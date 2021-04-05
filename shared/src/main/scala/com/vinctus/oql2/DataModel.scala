package com.vinctus.oql2

class DataModel(model: DMLModel, dml: String) {

  val entities: Map[String, Entity] = {
    var error = false

    def duplicates(ids: Seq[Ident], typ: String): Unit =
      ids.groupBy(_.s).toList filter { case (_, v) => v.length > 1 } flatMap { case (_, s) => s } match {
        case Nil =>
        case duplicates =>
          duplicates foreach { id =>
            printError(id.pos, s"duplicate $typ: '${id.s}'", dml)
          }

          error = true
      }

    duplicates(model.entities.map(_.name), "entity name")
    duplicates(model.entities.flatMap(_.alias map (List(_)) getOrElse Nil), "entity alias")

    for (entity <- model.entities) {
      duplicates(entity.attributes.map(_.name), "attribute name")
      duplicates(entity.attributes.flatMap(_.alias map (List(_)) getOrElse Nil), "attribute alias")
    }

    if (error)
      sys.error("duplicates found while creating data model")

    for (DMLEntity(name, alias, attributes) <- model.entities) {}

    Map()
  }

}

class Entity(name: String, table: String, attributes: Map[String, Attribute])

class Attribute(name: String)
