grammar DML;

@header {
  package com.vinctus.oql2;
}

model returns [DMLModel m]
  : entities
    { $m = new DMLModel($entities.es.toList()); }
  ;

entities returns [scala.collection.mutable.ListBuffer<DMLEntity> es]
  : l=entities entity
    { $es = $l.es.addOne($entity.e); }
  | entity
    { $es = new scala.collection.mutable.ListBuffer<DMLEntity>().addOne($entity.e); }
  ;

entity returns [DMLEntity e]
  : 'entity' entityName ('(' alias ')')? '{' attributes '}'
    { $e = new DMLEntity($entityName.id, null, $attributes.as.toList()); }
  ;

attributes returns [scala.collection.mutable.ListBuffer<DMLAttribute> as]
  : l=attributes attribute
    { $as = $l.as.addOne($attribute.a); }
  | attribute
    { $as = new scala.collection.mutable.ListBuffer<DMLAttribute>().addOne($attribute.a); }
  ;

attribute returns [DMLAttribute a]
  : pk? attributeName ('(' alias ')')? ':' attributeType required?
    { $a = new DMLAttribute($attributeName.id, null, null, false, false); }
  ;

pk
  : '*'
  ;

required
  : '!'
  ;

attributeType
  : primitiveType | manyToOneType | oneToManyType
  ;

primitiveType:
  'text' |
  'integer' | 'int' | 'int4' |
  'bool' | 'boolean' |
  'bigint' |
  'decimal' |
  'date' |
  'float' | 'float8' |
  'uuid' |
  'timestamp'
  ;

manyToOneType returns [Ident id]
  : entityName
    { $id = $entityName.id; }
  ;

oneToManyType
  : '[' entityName ('.' attributeName)? ']'
  ;

alias returns [Ident id]
  : identifier
    { $id = $identifier.id; }
  ;

entityName returns [Ident id]
  : identifier
    { $id = $identifier.id; }
  ;

attributeName returns [Ident id]
  : identifier
    { $id = $identifier.id; }
  ;

identifier returns [Ident id]
  : IDENTIFIER
    { $id = new Ident($IDENTIFIER.text, $IDENTIFIER.line, $IDENTIFIER.pos); }
  ;

IDENTIFIER
  : [A-Za-z_] [A-Za-z0-9_]*
  ;

WHITESPACE
  : [ \t\r\n]+ -> skip
  ;

COMMENT
  : '/*' (COMMENT | .)*? '*/' -> skip
  ;

LINE_COMMENT
  : '//' (~[\r\n])* -> skip
  ;
