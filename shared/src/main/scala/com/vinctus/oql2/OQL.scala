package com.vinctus.oql2

class OQL(dm: String, db: OQLDataSource) {

  val model: DataModel =
    DMLParse(dm) match {
      case None              => sys.error("error building data model")
      case Some(m: DMLModel) => new DataModel(m, dm)
    }

  def entity(name: String): Entity = model.entities(name)

  def queryMany(query: String, parameters: Map[String, Any]): collection.Seq[Any] = Nil

}
