package com.vinctus.oql

trait PGDataSource extends SQLDataSource {

  val host: String
  val port: Int
  val database: String
  val user: String
  val password: String

  def mapType(typ: TypeSpecifier): String =
    typ match {
      case TextType              => "TEXT"
      case IntegerType           => "INTEGER"
      case BooleanType           => "BOOLEAN"
      case BigintType            => "BIGINT"
      case DecimalType(p, s)     => s"NUMERIC($p, $s)"
      case DateType              => "DATE"
      case FloatType             => "DOUBLE PRECISION"
      case UUIDType              => "UUID"
      case TimestampType         => "TIMESTAMP WITHOUT TIME ZONE"
      case ManyToOneType(entity) => mapType(entity.pk.get.typ)
    }

  def mapPKType(typ: TypeSpecifier): String =
    typ match {
      case IntegerType => "SERIAL"
      case BigintType  => "BIGSERIAL"
      case _: Datatype => mapType(typ)
    }

  def reverseMapType(typ: String): Datatype =
    typ match {
      case "timestamp without time zone" => TimestampType
      case "uuid"                        => UUIDType
      case "integer"                     => IntegerType
      case "bigint"                      => BigintType
      case "double precision"            => FloatType
    }

  /*
  postgres=# select name, array(select array[name] from book where author_id = pk_author_id) from author;
          name          |                   array
------------------------+-------------------------------------------
 Robert Louis Stevenson | {{"Robert Louis Stevenson"}}
 Lewis Carroll          | {{"Lewis Carroll"}}
 Charles Dickens        | {{"Charles Dickens"},{"Charles Dickens"}}
 Mark Twain             | {{"Mark Twain"},{"Mark Twain"}}
   */
  val resultArrayFunctionStart: String = "array("
  val resultArrayFunctionEnd: String = ")"
  val rowSequenceFunctionStart: String = "row("
  val rowSequenceFunctionEnd: String = ")"
  val typeFunction: Option[String] = Some("pg_typeof(?)")
  val convertFunction: Option[String] = None
  val caseSensitive: Boolean = false
  val functionReturnType: Map[(String, Int), List[Datatype] => Datatype] =
    Map[(String, Int), List[Datatype] => Datatype](
      ("count", 1) -> (_ => IntegerType), // todo: this should really be 'BigintType'
      ("min", 1) -> (_.head),
      ("max", 1) -> (_.head),
      ("avg", 1) -> (_ => FloatType)
    )
  val builtinVariables = Map("current_date" -> DateType, "current_timestamp" -> TimestampType, "current_time" -> TimeType)

}
