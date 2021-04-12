package com.vinctus.oql2

import scala.collection.immutable.VectorMap
import scala.collection.mutable

class DataModel(model: DMLModel, dml: String) {

  private case class EntityInfo(entity: Entity,
                                dmlattrs: Seq[DMLAttribute],
                                attrs: mutable.LinkedHashMap[String, Attribute] = new mutable.LinkedHashMap)

  val entities: Map[String, Entity] = {
    error = false

    val entities = new mutable.LinkedHashMap[String, EntityInfo]

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

      entities((entity.alias getOrElse entity.name).s) = EntityInfo(Entity((entity.alias getOrElse entity.name).s, entity.name.s), entity.attributes)
    }

    for (EntityInfo(e, dmlas, as) <- entities.values) {
      var pk: Option[Attribute] = None

      for (a <- dmlas) {
        val typ =
          a.typ match {
            case DMLSimplePrimitiveType("text")                     => TextType
            case DMLSimplePrimitiveType("integer" | "int" | "int4") => IntegerType
            case DMLSimplePrimitiveType("bool" | "boolean")         => BooleanType
            case DMLSimplePrimitiveType("bigint")                   => BigintType
            case DMLParametricPrimitiveType("decimal", parameters)  => DecimalType(parameters.head.toInt, parameters.tail.head.toInt)
            case DMLSimplePrimitiveType("date")                     => DateType
            case DMLSimplePrimitiveType("float" | "float8")         => FloatType
            case DMLSimplePrimitiveType("uuid")                     => UUIDType
            case DMLSimplePrimitiveType("timestamp")                => TimestampType
            case DMLManyToOneType(typ) =>
              entities get typ.s match {
                case Some(EntityInfo(entity, _, _)) => ManyToOneType(entity)
                case None                           => printError(typ.pos, s"unknown entity: '${typ.s}'", dml)
              }
            case DMLOneToManyType(typ, attr) =>
              entities get typ.s match {
                case Some(EntityInfo(entity, _, _)) => OneToManyType(entity, null)
                case None                           => printError(typ.pos, s"unknown entity: '${typ.s}'", dml)
              }
          }

        val attr = Attribute((a.alias getOrElse a.name).s, a.name.s, a.pk, a.required, typ)

        if (a.pk) {
          if (!typ.isDataType)
            printError(typ.asInstanceOf[DMLEntityType].entity.pos, "primary key must be a non-relational data type", dml)

          if (a.required)
            printError(a.name.pos, "primary keys are already \"NOT NULL\" (required) by definition and may not be marked as such", dml)

          pk = Some(attr)
        }

        as((a.alias getOrElse a.name).s) = attr
      }

      e._pk = pk
    }

    for (EntityInfo(e, dmlas, as) <- entities.values) {
      dmlas.foreach {
        case DMLAttribute(_, _, DMLManyToOneType(entity), _, _) =>
          if (entities(entity.s).entity.pk.isEmpty)
            printError(entity.pos, s"target entity '${entity.s}' has no declared primary key", dml)
        case a @ DMLAttribute(_, _, DMLOneToManyType(typ, attr), _, _) =>
          val entityinfo = entities(typ.s)
          val newtyp =
            attr match {
              case Some(id) =>
                entityinfo.attrs get id.s match {
                  case Some(a @ Attribute(_, _, _, _, ManyToOneType(`e`))) => OneToManyType(entityinfo.entity, a)
                  case Some(_)                                             => printError(id.pos, s"attribute '${id.s}' of entity '${entityinfo.entity.name}' does not have the correct type", dml)
                  case None                                                => printError(id.pos, s"entity '${entityinfo.entity.name}' does not have attribute '${id.s}'", dml)
                }
              case None =>
                val attrs =
                  entityinfo.attrs.values.filter {
                    case Attribute(_, _, _, _, ManyToOneType(`e`)) => true
                    case _                                         => false
                  }

                if (attrs.size > 1)
                  printError(typ.pos, s"entity '${entityinfo.entity.name}' has more than one attribute of type '${typ.s}'", dml)

                if (attrs.size < 1)
                  printError(typ.pos, s"entity '${entityinfo.entity.name}' has no attributes of type '${typ.s}'", dml)

                OneToManyType(entityinfo.entity, attrs.head)
            }

          val name = (a.alias getOrElse a.name).s

          as(name) = as(name).copy(typ = newtyp)
        case DMLAttribute(_, _, _: DMLPrimitiveType, _, _) =>
      }

      e._attributes = as to VectorMap
    }

    if (error)
      sys.error("errors while creating data model")

    entities.view.mapValues(_.entity) to VectorMap
  }

}

case class Attribute(name: String, column: String, pk: Boolean, required: Boolean, typ: TypeSpecifier)
