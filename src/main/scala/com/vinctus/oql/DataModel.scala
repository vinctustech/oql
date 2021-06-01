package com.vinctus.oql

import scala.collection.immutable.VectorMap
import scala.collection.mutable

class DataModel(model: DMLModel, dml: String) {

  private case class EntityInfo(entity: Entity,
                                dmlattrs: Seq[DMLAttribute],
                                attrs: mutable.LinkedHashMap[String, Attribute] = new mutable.LinkedHashMap)

  val entities: Map[String, Entity] = {
    parsingError = false

    val entities = new mutable.LinkedHashMap[String, EntityInfo]

    def duplicates(ids: Seq[Ident], typ: String): Unit =
      ids.groupBy(_.s).toList filter { case (_, v) => v.length > 1 } flatMap { case (_, s) => s } match {
        case Nil        =>
        case duplicates => duplicates foreach (id => printError(id.pos, s"duplicate $typ name: '${id.s}'", dml))
      }

    def unknownEntity(typ: Ident) = printError(typ.pos, s"unknown entity: '${typ.s}'", dml)

    duplicates(model.entities.map(e => e.actualName getOrElse e.name), "actual table")
    duplicates(model.entities.map(_.name), "entity")

    for (entity <- model.entities) {
      duplicates(entity.attributes.map(a => a.actualName getOrElse a.name), " actual column")
      duplicates(model.entities.map(_.name), "attribute")

      entity.attributes.filter(_.pk) match {
        case as if as.length > 1 => as foreach (a => printError(a.name.pos, s"extraneous primary key", dml))
        case _                   =>
      }

      entities(entity.name.s) = EntityInfo(Entity(entity.name.s, (entity.actualName getOrElse entity.name).s), entity.attributes)
    }

    for (EntityInfo(e, dmlas, as) <- entities.values) {
      var pk: Option[Attribute] = None

      for (a <- dmlas) {
        val typ =
          a.typ match {
            case DMLSimpleDataType("text")                     => TextType
            case DMLSimpleDataType("integer" | "int" | "int4") => IntegerType
            case DMLSimpleDataType("bool" | "boolean")         => BooleanType
            case DMLSimpleDataType("bigint")                   => BigintType
            case DMLParametricDataType("decimal", parameters)  => DecimalType(parameters.head.toInt, parameters.tail.head.toInt)
            case DMLSimpleDataType("date")                     => DateType
            case DMLSimpleDataType("float" | "float8")         => FloatType
            case DMLSimpleDataType("uuid")                     => UUIDType
            case DMLSimpleDataType("timestamp")                => TimestampType
            case DMLManyToOneType(typ) =>
              entities get typ.s match {
                case Some(EntityInfo(entity, _, _)) => ManyToOneType(entity)
                case None                           => unknownEntity(typ)
              }
            case DMLOneToOneType(typ, attr) =>
              entities get typ.s match {
                case Some(EntityInfo(entity, _, _)) => OneToOneType(entity, null)
                case None                           => unknownEntity(typ)
              }
            case DMLOneToManyType(typ, attr) =>
              entities get typ.s match {
                case Some(EntityInfo(entity, _, _)) => OneToManyType(entity, null)
                case None                           => unknownEntity(typ)
              }
            case DMLManyToManyType(entity, link) =>
              (entities get entity.s, entities get link.s) match {
                case (Some(EntityInfo(entity, _, _)), Some(EntityInfo(link, _, _))) => ManyToManyType(entity, link, null, null)
                case (e, l) =>
                  if (e.isEmpty)
                    unknownEntity(entity)
                  if (l.isEmpty)
                    unknownEntity(link)

                  null
              }
          }

        val attr = Attribute(a.name.s, (a.actualName getOrElse a.name).s, a.pk, a.required, typ)

        if (a.pk) {
          if (!typ.isDataType)
            printError(typ.asInstanceOf[DMLEntityType].entity.pos, "primary key must be a DBMS data type", dml)
          if (a.required)
            printError(a.name.pos, "primary keys are \"NOT NULL\" (required) by definition", dml)

          pk = Some(attr)
        }

        as(a.name.s) = attr
      }

      e._pk = pk
    }

    for (EntityInfo(e, dmlas, as) <- entities.values) {
      dmlas.foreach {
        case a @ DMLAttribute(_, _, DMLManyToManyType(entity, link), _, _) =>
          val linkinfo = entities(link.s)
          val self =
            linkinfo.attrs.values.filter {
              case Attribute(_, _, _, _, ManyToOneType(`e`)) => true
              case _                                         => false
            }

          if (self.size > 1)
            printError(link.pos, s"junction entity '${linkinfo.entity.name}' has more than one attribute of type '${link.s}'", dml)

          if (self.size < 1)
            printError(link.pos, s"junction entity '${linkinfo.entity.name}' has no attributes of type '${link.s}'", dml)

          val targetentity = entities(entity.s).entity
          val target =
            linkinfo.attrs.values.filter {
              case Attribute(_, _, _, _, ManyToOneType(`targetentity`)) => true
              case _                                                    => false
            }

          if (target.size > 1)
            printError(link.pos, s"junction entity '${linkinfo.entity.name}' has more than one attribute of type '${link.s}'", dml)

          if (target.size < 1)
            printError(link.pos, s"junction entity '${linkinfo.entity.name}' has no attributes of type '${link.s}'", dml)

          as(a.name.s) = as(a.name.s).copy(typ = ManyToManyType(targetentity, linkinfo.entity, self.head, target.head))
        case DMLAttribute(_, _, DMLManyToOneType(entity), _, _) =>
          if (entities(entity.s).entity.pk.isEmpty)
            printError(entity.pos, s"target entity '${entity.s}' has no declared primary key", dml)
        case a @ DMLAttribute(_, _, DMLOneToOneType(typ, attr), _, _) =>
          val entityinfo = entities(typ.s)
          val newtyp =
            attr match {
              case Some(id) =>
                entityinfo.attrs get id.s match {
                  case Some(a @ Attribute(_, _, _, _, ManyToOneType(`e`))) => OneToOneType(entityinfo.entity, a)
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

                OneToOneType(entityinfo.entity, attrs.head)
            }

          as(a.name.s) = as(a.name.s).copy(typ = newtyp)
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

          as(a.name.s) = as(a.name.s).copy(typ = newtyp)
        case DMLAttribute(_, _, _: DMLDataType, _, _) =>
      }

      e._attributes = as to VectorMap
    }

    if (parsingError)
      sys.error("errors while creating data model")

    entities.view.mapValues(_.entity) to VectorMap
  }

}

case class Attribute(name: String, column: String, pk: Boolean, required: Boolean, typ: TypeSpecifier)
