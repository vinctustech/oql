package com.vinctus.oql2

trait SQLDataSource extends OQLDataSource {

  def mapType(typ: TypeSpecifier): String

  def mapPKType(typ: TypeSpecifier): String

  def schema(model: DataModel): Seq[String] = {
    val q = '"'
    val tables =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield {
          val columns =
            for (attribute <- entity.attributes.values if attribute.typ.isColumnType)
              yield
                if (attribute.pk)
                  s"  $q${attribute.column}$q ${mapPKType(attribute.typ)} PRIMARY KEY"
                else
                  s"  $q${attribute.column}$q ${mapType(attribute.typ)}${if (attribute.required) " NOT NULL" else ""}"

          s"""
             |CREATE TABLE "${entity.table}" (
             |${columns mkString ",\n"}
             |);""".trim.stripMargin
        }

    val foreignKeys =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield
          for (attribute <- entity.attributes.values if attribute.typ.isInstanceOf[ManyToOneType])
            yield
              s"ALTER TABLE $q${entity.table}$q ADD FOREIGN KEY ($q${attribute.column}$q) REFERENCES $q${attribute.typ.asInstanceOf[ManyToOneType].entity.table}$q;"

    tables ++ foreignKeys.flatten
  }

  val typeFunction: Option[String]
  val convertFunction: Option[String]
  val resultArrayFunctionStart: String
  val resultArrayFunctionEnd: String
  val rowSequenceFunctionStart: String
  val rowSequenceFunctionEnd: String
  val functionReturnType: Map[String, DataType]

  def reverseMapType(typ: String): DataType

  val platformSpecific: PartialFunction[Any, String]

}
