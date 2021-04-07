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
      var pk: Option[Attribute] = None
      val attributes =
        for (a <- as)
          yield {
            val typ =
              a.typ match {
                case DMLSimplePrimitiveType("text")                     => TextType
                case DMLSimplePrimitiveType("integer" | "int" | "int4") => IntegerType
                case DMLSimplePrimitiveType("bool" | "boolean")         => BooleanType
                case DMLSimplePrimitiveType("bigint")                   => BigintType
                case DMLParametricPrimitiveType("decimal", parameters) =>
                  DecimalType(parameters.head.toInt, parameters.tail.head.toInt)
                case DMLSimplePrimitiveType("date")             => DateType
                case DMLSimplePrimitiveType("float" | "float8") => FloatType
                case DMLSimplePrimitiveType("uuid")             => UUIDType
                case DMLSimplePrimitiveType("timestamp")        => TimestampType
                case DMLManyToOneType(typ) =>
                  entities get typ.s match {
                    case Some(t) => ManyToOneType(typ.s, t._1)
                    case None    => printError(typ.pos, s"unknown entity: '${typ.s}'", dml)
                  }
              }

            val attr = Attribute((a.alias getOrElse a.name).s, a.name.s, a.pk, a.required, typ)

            if (a.pk) {
              if (!typ.isInstanceOf[PrimitiveType])
                printError(typ.asInstanceOf[DMLEntityType].entity.pos, "primary key must have primitive type", dml)

              if (a.required)
                printError(
                  a.name.pos,
                  "primary keys are already \"NOT NULL\" (required) by definition and may not be marked as such",
                  dml)

              pk = Some(attr)
            }

            ((a.alias getOrElse a.name).s, attr)
          }

      e._attributes = attributes to VectorMap
      e._pk = pk
    }

    for ((_, as) <- entities.values) {
      as.foreach {
        case DMLAttribute(_, _, DMLManyToOneType(entity), false, _) =>
          if (entities(entity.s)._1.pk.isEmpty)
            printError(entity.pos, s"target entity '${entity.s}' has no declared primary key", dml)
        case _ =>
      }
    }

    if (error)
      sys.error("errors while creating data model")

    entities.view.mapValues(_._1).toMap
  }

}

case class Attribute(name: String, column: String, pk: Boolean, required: Boolean, typ: TypeSpecifier)

trait TypeSpecifier
trait PrimitiveType extends TypeSpecifier

case object TextType extends PrimitiveType
case object IntegerType extends PrimitiveType
case object BooleanType extends PrimitiveType
case object BigintType extends PrimitiveType
case class DecimalType(precision: Int, scale: Int) extends PrimitiveType
case object DateType extends PrimitiveType
case object FloatType extends PrimitiveType
case object UUIDType extends PrimitiveType
case object TimestampType extends PrimitiveType

case class ManyToOneType(entityName: String, entity: Entity) extends TypeSpecifier
