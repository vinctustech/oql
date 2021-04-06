package com.vinctus.oql2

import scala.collection.immutable.VectorMap
import scala.collection.mutable

class DataModel(model: DMLModel, dml: String) {

  val entities: Map[String, Entity] = {
    error = false

    val entities = new mutable.HashMap[String, (Entity, Seq[DMLAttribute])]

    def duplicates(ids: Seq[Ident], typ: String): Unit =
      ids.groupBy(_.s).toList filter { case (_, v) => v.length > 1 } flatMap { case (_, s) => s } match {
        case Nil        =>
        case duplicates => duplicates foreach (id => printError(id.pos, s"duplicate $typ name: '${id.s}'", dml))
      }

    duplicates(model.entities.map(e => e.alias getOrElse e.name), "effective entity")
    duplicates(model.entities.map(_.name), "entity")

    for (entity <- model.entities) {
      duplicates(entity.attributes.map(a => a.alias getOrElse a.name), " effective attribute")
      duplicates(model.entities.map(_.name), "attribute")

      entity.attributes.filter(_.pk) match {
        case as if as.length > 1 => as foreach (a => printError(a.name.pos, s"extraneous primary key", dml))
        case _                   =>
      }

      entities((entity.alias getOrElse entity.name).s) =
        (new Entity((entity.alias getOrElse entity.name).s, entity.name.s), entity.attributes)
    }

    for ((e, as) <- entities.values) {
      val attributes =
        for (a <- as)
          yield {
            val typ =
              a.typ match {
                case DMLPrimitiveType("text")                     => TextType
                case DMLPrimitiveType("integer" | "int" | "int4") => IntegerType
                case DMLPrimitiveType("bool" | "boolean")         => BooleanType
                case DMLPrimitiveType("bigint")                   => BigintType
                case DMLPrimitiveType("decimal")                  => DecimalType
                case DMLPrimitiveType("date")                     => DateType
                case DMLPrimitiveType("float" | "float8")         => FloatType
                case DMLPrimitiveType("uuid")                     => UUIDType
                case DMLPrimitiveType("timestamp")                => TimestampType
                case DMLManyToOneType(typ) =>
                  entities get typ.s match {
                    case Some(t) => ManyToOneType(typ.s, t._1)
                    case None    => printError(typ.pos, s"unknown entity: '${typ.s}'", dml)
                  }
              }

            ((a.alias getOrElse a.name).s, new Attribute((a.alias getOrElse a.name).s, a.name.s, a.pk, a.required, typ))
          }

      e._attributes = attributes to VectorMap
    }

    if (error)
      sys.error("errors while creating data model")

    entities.view.mapValues(_._1).toMap
  }

}

class Entity(name: String, table: String) {
  var _attributes: Map[String, Attribute] = _
  lazy val attributes: Map[String, Attribute] = _attributes
}

class Attribute(name: String, column: String, pk: Boolean, required: Boolean, typ: TypeSpecifier)

trait TypeSpecifier
trait PrimitiveType extends TypeSpecifier

case object TextType extends PrimitiveType
case object IntegerType extends PrimitiveType
case object BooleanType extends PrimitiveType
case object BigintType extends PrimitiveType
case object DecimalType extends PrimitiveType
case object DateType extends PrimitiveType
case object FloatType extends PrimitiveType
case object UUIDType extends PrimitiveType
case object TimestampType extends PrimitiveType

case class ManyToOneType(typ: String, entity: Entity) extends TypeSpecifier
