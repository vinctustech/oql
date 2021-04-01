package com.vinctus.oql2

case class DMLModel(entities: Seq[DMLEntity])
case class DMLEntity(name: String)

//case class EntityAttributeERD(attr: Ident, actualField: Ident, typ: TypeSpecifierERD, pk: Boolean, required: Boolean)
