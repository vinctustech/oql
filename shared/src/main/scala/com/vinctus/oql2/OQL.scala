package com.vinctus.oql2

class OQL(dm: String, db: DataSource) {

  val model: DataModel =
    DMLParse(dm) match {
      case None              => sys.error("error building data model")
      case Some(m: DMLModel) => new DataModel(m, dm)
    }

  def queryMany(query: String, parameters: Map[String, Any]): collection.Seq[Any] =

}
