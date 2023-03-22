package com.vinctus.oql

import scala.annotation.tailrec
import scala.collection.immutable.VectorMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DataModel(model: DMLModel, dml: String) {

  private case class EntityInfo(
      entity: Entity,
      dmlattrs: Seq[DMLAttribute],
      attrs: mutable.LinkedHashMap[String, Attribute] = new mutable.LinkedHashMap
  )

  private var first: Entity = _

  val (entities: Map[String, Entity], enums: Map[String, EnumType]) = {
    parsingError = false

    val enums = new mutable.HashMap[String, EnumType]
    val entities = new mutable.LinkedHashMap[String, EntityInfo]

    def duplicates(ids: Seq[Ident], typ: String): Unit =
      ids.groupBy(_.s).toList filter { case (_, v) => v.length > 1 } flatMap { case (_, s) => s } match {
        case Nil        =>
        case duplicates => duplicates foreach (id => printError(id.pos, s"duplicate $typ name: '${id.s}'", dml))
      }

    def unknownEntity(typ: Ident) = printError(typ.pos, s"unknown entity: '${typ.s}'", dml)

    def entityDecls = model.decls.filter(_.isInstanceOf[DMLEntity]).asInstanceOf[Seq[DMLEntity]]

    def enumDecls = model.decls.filter(_.isInstanceOf[DMLEnum]).asInstanceOf[Seq[DMLEnum]]

    duplicates(enumDecls.map(_.name), "enum")

    for (e <- enumDecls) {
      duplicates(e.labels, "label")
      enums(e.name.s) = EnumType(e.name.s, e.labels map (_.s))
    }

    duplicates(entityDecls.map(e => e.actualName getOrElse e.name), "actual table")
    duplicates(entityDecls.map(_.name), "entity")

    for (entity <- entityDecls) {
      duplicates(entity.attributes.map(a => a.actualName getOrElse a.name), "actual column")
      duplicates(entityDecls.map(_.name), "attribute")

      entity.attributes.filter(_.pk) match {
        case as if as.length > 1 => as foreach (a => printError(a.name.pos, s"extraneous primary key", dml))
        case _                   =>
      }

      val info = EntityInfo(Entity(entity.name.s, (entity.actualName getOrElse entity.name).s), entity.attributes)

      if (first eq null) // todo: all fixable entities must have a declared primary key
        first = info.entity

      entities(entity.name.s) = info
    }

    for (EntityInfo(e, dmlas, as) <- entities.values) {
      var pk: Option[Attribute] = None

      for (a <- dmlas) {
        val typ =
          a.typ match {
            case DMLSimpleDataType("text")                     => TextType
            case DMLSimpleDataType("integer" | "int" | "int4") => IntegerType
            case DMLSimpleDataType("bool" | "boolean")         => BooleanType
            case DMLSimpleDataType("bigint" | "int8")          => BigintType
            case DMLParametricDataType("decimal", parameters) =>
              DecimalType(parameters.head.toInt, parameters.tail.head.toInt)
//            case DMLSimpleDataType("date")                     => DateType
            case DMLSimpleDataType("float" | "float8") => FloatType
            case DMLSimpleDataType("uuid")             => UUIDType
            case DMLSimpleDataType("timestamp")        => TimestampType
            case DMLSimpleDataType("json")             => JSONType
            case DMLNameType(typ) =>
              entities get typ.s match {
                case Some(EntityInfo(entity, _, _)) => ManyToOneType(entity)
                case None =>
                  enums get typ.s match {
                    case Some(e) => e
                    case None    => unknownEntity(typ)
                  }
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
                case (Some(EntityInfo(entity, _, _)), Some(EntityInfo(link, _, _))) =>
                  ManyToManyType(entity, link, null, null)
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
            printError(
              link.pos,
              s"junction entity '${linkinfo.entity.name}' has more than one attribute of type '${link.s}'",
              dml
            )

          if (self.size < 1)
            printError(
              link.pos,
              s"junction entity '${linkinfo.entity.name}' has no attributes of type '${link.s}'",
              dml
            )

          val targetentity = entities(entity.s).entity
          val target =
            linkinfo.attrs.values.filter {
              case Attribute(_, _, _, _, ManyToOneType(`targetentity`)) => true
              case _                                                    => false
            }

          if (target.size > 1)
            printError(
              link.pos,
              s"junction entity '${linkinfo.entity.name}' has more than one attribute of type '${link.s}'",
              dml
            )

          if (target.size < 1)
            printError(
              link.pos,
              s"junction entity '${linkinfo.entity.name}' has no attributes of type '${link.s}'",
              dml
            )

          as(a.name.s) = as(a.name.s).copy(typ = ManyToManyType(targetentity, linkinfo.entity, self.head, target.head))
        case DMLAttribute(_, _, DMLNameType(entity), _, _) =>
          if (entities.contains(entity.s) && entities(entity.s).entity.pk.isEmpty)
            printError(entity.pos, s"target entity '${entity.s}' has no declared primary key", dml)
        case a @ DMLAttribute(_, _, DMLOneToOneType(typ, attr), _, _) =>
          val entityinfo = entities(typ.s)
          val newtyp =
            attr match {
              case Some(id) =>
                entityinfo.attrs get id.s match {
                  case Some(a @ Attribute(_, _, _, _, ManyToOneType(`e`))) => OneToOneType(entityinfo.entity, a)
                  case Some(_) =>
                    printError(
                      id.pos,
                      s"attribute '${id.s}' of entity '${entityinfo.entity.name}' does not have the correct type",
                      dml
                    )
                  case None =>
                    printError(id.pos, s"entity '${entityinfo.entity.name}' does not have attribute '${id.s}'", dml)
                }
              case None =>
                val attrs =
                  entityinfo.attrs.values.filter {
                    case Attribute(_, _, _, _, ManyToOneType(`e`)) => true
                    case _                                         => false
                  }

                if (attrs.size > 1)
                  printError(
                    typ.pos,
                    s"entity '${entityinfo.entity.name}' has more than one attribute of type '${e.name}'",
                    dml
                  )

                if (attrs.size < 1)
                  printError(typ.pos, s"entity '${entityinfo.entity.name}' has no attributes of type '${e.name}'", dml)

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
                  case Some(_) =>
                    printError(
                      id.pos,
                      s"attribute '${id.s}' of entity '${entityinfo.entity.name}' does not have the correct type",
                      dml
                    )
                  case None =>
                    printError(id.pos, s"entity '${entityinfo.entity.name}' does not have attribute '${id.s}'", dml)
                }
              case None =>
                val attrs =
                  entityinfo.attrs.values.filter {
                    case Attribute(_, _, _, _, ManyToOneType(`e`)) => true
                    case _                                         => false
                  }

                if (attrs.size > 1)
                  printError(
                    typ.pos,
                    s"entity '${entityinfo.entity.name}' has more than one attribute of type '${e.name}'",
                    dml
                  )

                if (attrs.size < 1)
                  printError(typ.pos, s"entity '${entityinfo.entity.name}' has no attributes of type '${e.name}'", dml)

                OneToManyType(entityinfo.entity, attrs.head)
            }

          as(a.name.s) = as(a.name.s).copy(typ = newtyp)
        case DMLAttribute(_, _, _: DMLDataType, _, _) =>
      }

      e._attributes = as to VectorMap
    }

    if (parsingError)
      sys.error("errors while creating data model")

    (entities.view.mapValues(_.entity) to VectorMap, enums.toMap)
  }

  // fixed entity processing

  for (e <- entities.values) {
    val idsbuf = new ListBuffer[List[String]]
    val nullablesbuf = new ListBuffer[List[String]]

    def scan(attrs: List[String], ents: List[Entity], entity: Entity): Unit =
      if (entity == first && entity.pk.isDefined)
        idsbuf += (entity.pk.get.name :: attrs).reverse
      else
        for (Attribute(name, _, pk, required, typ) <- entity.attributes.values if !pk)
          typ match {
            case ManyToOneType(mtoEntity) =>
              val newents = entity :: ents

              if (!(newents contains mtoEntity)) { // avoid circularity
                if (!required)
                  nullablesbuf += (name :: attrs).reverse

                scan(name :: attrs, newents, mtoEntity)
              }
            case _ =>
          }

    scan(Nil, Nil, e)

    val attrs =
      idsbuf.toList map { ids =>
        val attridents = ids map (id => Ident(id))
        val attr = AttributeOQLExpression(attridents)

        attr.dmrefs = lookup(attr, attridents, ref = false, e, null)

        val nullables =
          nullablesbuf.toList filter (ids startsWith _) map { ids =>
            val nullidents = ids map (id => Ident(id))
            val nullable = ReferenceOQLExpression(nullidents)

            nullable.dmrefs = lookup(nullable, nullidents, ref = true, e, null)
            nullable
          }

        (attr, nullables)
      }

    e._fixing = Map(first -> attrs)
  }

  def lookup(
      expr: OQLExpression,
      ids: List[Ident],
      ref: Boolean,
      entity: Entity,
      input: String
  ): List[(Entity, Attribute)] = { // todo: code duplication
    val dmrefs = new ListBuffer[(Entity, Attribute)]

    @tailrec
    def lookup(ids: List[Ident], entity: Entity): Unit =
      ids match {
        case List(id) =>
          entity.attributes get id.s match {
            case Some(attr) =>
              dmrefs += (entity -> attr)

              if (ref) {
                if (!attr.typ.isInstanceOf[ManyToOneType])
                  problem(id.pos, s"attribute '${id.s}' is not many-to-one", input)

                expr.typ = attr.typ.asInstanceOf[ManyToOneType].entity.pk.get.typ.asDatatype
              } else {
                if (!attr.typ.isDataType)
                  problem(id.pos, s"attribute '${id.s}' is not a DBMS data type", input)

                expr.typ = attr.typ.asDatatype
              }
            case None => problem(id.pos, s"entity '${entity.name}' does not have attribute '${id.s}'", input)
          }
        case head :: tail =>
          entity.attributes get head.s match {
            case Some(attr @ Attribute(name, column, pk, required, ManyToOneType(mtoEntity))) =>
              dmrefs += (mtoEntity -> attr)
              lookup(tail, mtoEntity)
            case Some(_) =>
              problem(head.pos, s"attribute '${head.s}' of entity '${entity.name}' does not have an entity type", input)
            case None => problem(head.pos, s"entity '${entity.name}' does not have attribute '${head.s}'", input)
          }
      }

    lookup(ids, entity)
    dmrefs.toList
  }

}

case class Attribute(name: String, column: String, pk: Boolean, required: Boolean, typ: TypeSpecifier)
