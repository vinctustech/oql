package com.vinctus.oql2

trait SQLOQLDataSource extends OQLDataSource {

  def mapType(typ: TypeSpecifier): String

  def mapPKType(typ: PrimitiveType): String

  def create(model: DataModel): Unit = {
    val conn = connect

    schema(model) foreach conn.execute
    conn.close()
  }

  def schema(model: DataModel): Seq[String] = {
    val tables =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield {
          val columns =
            for (attribute <- entity.attributes.values)
              yield
                if (attribute.pk)
                  s"  ${attribute.column} ${mapPKType(attribute.typ.asInstanceOf[PrimitiveType])} PRIMARY KEY"
                else
                  s"  ${attribute.column} ${mapType(attribute.typ)}${if (attribute.required) " NOT NULL" else ""}"

          s"""
             |CREATE TABLE ${entity.table} (
             |${columns mkString ",\n"}
             |);""".trim.stripMargin
        }

    val foreignKeys =
      for (entity <- model.entities.values.toList.sortBy(_.table))
        yield
          for (attribute <- entity.attributes.values if attribute.typ.isInstanceOf[ManyToOneType])
            yield
              s"ALTER TABLE ${entity.table} ADD FOREIGN KEY (${attribute.column}) REFERENCES ${attribute.typ.asInstanceOf[ManyToOneType].entity.table};"

    tables ++ foreignKeys.flatten
  }

}
