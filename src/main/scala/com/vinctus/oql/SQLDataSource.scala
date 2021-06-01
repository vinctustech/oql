package com.vinctus.oql

trait SQLDataSource extends OQLDataSource {

  def mapType(typ: TypeSpecifier): String

  def mapPKType(typ: TypeSpecifier): String

  def schema(model: DataModel): Seq[String] = {
    val tables =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield {
          val columns =
            for (attribute <- entity.attributes.values if attribute.typ.isColumnType)
              yield
                if (attribute.pk)
                  s"  \"${attribute.column}\" ${mapPKType(attribute.typ)} PRIMARY KEY"
                else
                  s"  \"${attribute.column}\" ${mapType(attribute.typ)}${if (attribute.required) " NOT NULL" else ""}"

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
              s"ALTER TABLE \"${entity.table}\" ADD FOREIGN KEY (\"${attribute.column}\") REFERENCES \"${attribute.typ.asInstanceOf[ManyToOneType].entity.table}\";"

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

  val builtinVariables: Map[String, DataType]

  def quote(s: String): String

}
