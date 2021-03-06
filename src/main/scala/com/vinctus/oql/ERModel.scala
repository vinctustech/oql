package com.vinctus.oql

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.util.parsing.input.Position

class ERModel(defn: String) {

  val entities: Map[String, Entity] = {
    val erDefinition = ERDParser.parseDefinition(defn)
    val entityMap = new mutable.HashMap[String, Entity]

    erDefinition.blocks foreach {
      case EntityBlockERD(entity, _, _) =>
        if (entityMap contains entity.name)
          problem(entity.pos, s"entity '${entity.name}' already defined")
        else
          entityMap(entity.name) = new Entity(entity.name, null, null, null, null)
      case _ =>
    }

    erDefinition.blocks foreach {
      case EntityBlockERD(entity, actual, fields) =>
        var epk: String = null
        var epkcolumn: String = null
        val attrs = mutable.LinkedHashMap.empty[String, EntityAttribute]

        for (EntityAttributeERD(attr, column, typ, pk, required) <- fields) {
          if (attrs contains attr.name)
            problem(attr.pos, s"attribute '${attr.name}' already exists for this entity")
          else
            attrs.find {
              case (_, v) =>
                v.isInstanceOf[EntityColumnAttribute] && v
                  .asInstanceOf[EntityColumnAttribute]
                  .column == column.name
            } match {
              case Some((k, _)) =>
                problem(attr.pos, s"attribute '${attr.name}' refers to the same column as attribute '$k'")
              case None =>
                val fieldtype =
                  typ match {
                    case SimpleTypeERD(typ) =>
                      entityMap get typ.name match {
                        case Some(e) => ObjectEntityAttribute(column.name, typ.name, e, required)
                        case None =>
                          typ2db(typ.name) match {
                            case None    => problem(typ.pos, s"type '${typ.name}' not recognized")
                            case Some(t) => PrimitiveEntityAttribute(column.name, t, required)
                          }
                      }
                    case OneToOneTypeERD(typ, attr) =>
                      entityMap get typ.name match {
                        case Some(t) => ObjectOneEntityAttribute(typ.name, t, attr map (_.name))
                        case None    => problem(typ.pos, s"not an entity: ${typ.name}")
                      }
                    case JunctionArrayTypeERD(typ, attr, junction) =>
                      (entityMap get typ.name, entityMap get junction.name) match {
                        case (Some(t), Some(j)) =>
                          ObjectArrayJunctionEntityAttribute(typ.name, t, attr map (_.name), junction.name, j)
                        case (None, _) => problem(typ.pos, s"not an entity: ${typ.name}")
                        case (_, None) => problem(junction.pos, s"not an entity: ${junction.name}")
                      }
                    case ArrayTypeERD(typ, attr) =>
                      entityMap get typ.name match {
                        case Some(t) => ObjectArrayEntityAttribute(typ.name, t, attr map (_.name))
                        case None    => problem(typ.pos, s"not an entity: ${typ.name}")
                      }
                    case LiteralTypeERD(value) => LiteralEntityAttribute(eval(value))
                  }

                attrs += (attr.name -> fieldtype)
            }

          if (pk) {
            if (epk ne null)
              problem(attr.pos, "there is already a primary key defined for this entity")
            else {
              epk = attr.name
              epkcolumn = column.name
            }
          }
        }

        entityMap(entity.name).table = actual.name
        entityMap(entity.name).pk = if (epk ne null) Some(epk) else None
        entityMap(entity.name).pkcolumn = if (epkcolumn ne null) Some(epkcolumn) else None
        entityMap(entity.name).attributes = attrs.to(ListMap)
    }

    entityMap.toMap
  }

  def eval(expr: ExpressionERD): Any =
    expr match {
      case NullLiteralERD             => null
      case FloatLiteralERD(n)         => n.toDouble
      case StringLiteralERD(s)        => s
      case IntegerLiteralERD(n)       => n.toDouble
      case BooleanLiteralERD("true")  => true
      case BooleanLiteralERD("false") => false
      case ArrayLiteralERD(elems)     => elems map eval
      case ObjectLiteralERD(membs)    => membs map { case (k, v) => (eval(k), eval(v)) } to ListMap
    }

  def get(resource: String): Option[Entity] = entities get resource

  def get(resource: String, pos: Position): Entity =
    entities get resource match {
      case None    => problem(pos, s"unknown resource: '$resource'")
      case Some(e) => e
    }

  def list(resource: String, pos: Position): Seq[(String, EntityAttribute)] =
    get(resource, pos).attributes.toList

}
