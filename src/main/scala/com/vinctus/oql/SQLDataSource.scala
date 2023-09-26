package com.vinctus.oql

trait SQLDataSource extends OQLDataSource {

  def mapType(typ: TypeSpecifier): String

  def mapPKType(typ: TypeSpecifier): String

  def schema(model: DataModel): String = {
    val enums =
      model.enums.values.toSeq.sortBy(_.name).map { case EnumType(name, labels) =>
        s"CREATE TYPE \"$name\" AS ENUM (${labels map (l => s"'$l'") mkString ", "});\n"
      }

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
             |);
             |""".trim.stripMargin
        }

//    val foreignKeys =
//      for (entity <- model.entities.values.toList.sortBy(_.table))
//        yield for (attribute <- entity.attributes.values if attribute.typ.isInstanceOf[ManyToOneType])
//          yield s"ALTER TABLE \"${entity.table}\" ADD FOREIGN KEY (\"${attribute.column}\") REFERENCES \"${attribute.typ.asInstanceOf[ManyToOneType].entity.table}\";\n"
//
//    (tables ++ foreignKeys.flatten) mkString
    enums.mkString ++ tables.mkString
  }

  val typeFunction: Option[String]
  val convertFunction: Option[String]
  val resultArrayFunctionStart: String
  val resultArrayFunctionEnd: String
  val rowSequenceFunctionStart: String
  val rowSequenceFunctionEnd: String
  val functionReturnType: Map[(String, Int), Seq[Datatype] => Datatype]
  val caseSensitive: Boolean

  def reverseMapType(typ: String): Datatype

  val platformSpecific: PartialFunction[Any, String]

  val builtinVariables: Map[String, Datatype]

  def quote(s: String): String =
    s.replace("\\", """\\""")
      .replace("'", """\'""")
      .replace("\t", """\t""")
      .replace("\r", """\r""")
      .replace("\n", """\n""")

  def string(s: String): String =
    val quoted = quote(s)

    if quoted == s then s"'$s'" else s"E'$quoted'"

  def typed(a: Any, typ: Datatype): String =
    (a, typ) match {
      case (s: String, UUIDType)      => s"$s::UUID"
      case (s: String, IntegerType)   => s"$s::INTEGER"
      case (s: String, SmallintType)  => s"$s::SMALLINT"
      case (s: String, BooleanType)   => s"$s::BOOLEAN"
      case (s: String, BigintType)    => s"$s::BIGINT"
      case (s: String, FloatType)     => s"$s::DOUBLE PRECISION"
      case (s: String, TimestampType) => s"$s::TIMESTAMP"
      case (s: String, DateType)      => s"$s::DATE"
      case (s: String, TimeType)      => s"$s::TIME"
      case (s: String, JSONType)      => s"$s::JSON"
      case (s: String, IntervalType)  => s"$s::INTERVAL"
      case _ =>
        Console.err.println(s"WARNING: SQLDataSource.typed(): don't know how to render '$a' as type $typ")
        a.toString
    }

}
