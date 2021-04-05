package com.vinctus.oql2

class DataModel(model: DMLModel, dml: String) {

  require(toSet.size == model.entities.length, "require entity names to be unique")

  val entities: Map[String, Entity] = {
    model.entities.map(_.name).groupBy(identity).toList.map { case (k, v) => (k, v.length) }
  }

}

class Entity(name: String, table: String, attributes: Map[String, Attribute])

class Attribute(name: String)
