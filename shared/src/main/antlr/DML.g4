grammar DML;

@header {
  package com.vinctus.oql2;

  import scala.collection.mutable.ListBuffer;
}

model returns [DMLModel m]
  : entities
    { $m = new DMLModel($entities.es.toList()); }
  ;

entities returns [ListBuffer<DMLEntity> es]
  : l=entities entity
    { $es = $l.es.addOne($entity.e); }
  | entity
    { $es = new ListBuffer<DMLEntity>().addOne($entity.e); }
  ;

entity returns [DMLEntity e]
  : 'entity' entityName ('(' alias ')')? '{' attributes '}'
    { $e = new DMLEntity($entityName.id, DMLParse.alias($alias.ctx), $attributes.as.toList()); }
  ;

attributes returns [ListBuffer<DMLAttribute> as]
  : l=attributes attribute
    { $as = $l.as.addOne($attribute.a); }
  | attribute
    { $as = new ListBuffer<DMLAttribute>().addOne($attribute.a); }
  ;

attribute returns [DMLAttribute a]
  : pk? attributeName ('(' alias ')')? ':' attributeType required?
    { $a = new DMLAttribute($attributeName.id, DMLParse.alias($alias.ctx), $attributeType.t, $pk.ctx != null, $required.ctx != null); }
  ;

pk
  : '*'
  ;

required
  : '!'
  ;

attributeType returns [DMLTypeSpecifier t]
  : primitiveType
    { $t = $primitiveType.t; }
  | manyToOneType
    { $t = $manyToOneType.t; }
  | oneToManyType
    { $t = $oneToManyType.t; }
  | manyToManyType
    { $t = $manyToManyType.t; }
  ;

primitiveType returns [DMLPrimitiveType t]
  : s=(
    'text' |
    'integer' | 'int' | 'int4' |
    'bool' | 'boolean' |
    'bigint' |
    'decimal' |
    'date' |
    'float' | 'float8' |
    'uuid' |
    'timestamp'
    )
    { $t = new DMLPrimitiveType($s.text); }
  ;

manyToOneType returns [DMLManyToOneType t]
  : entityName
    { $t = new DMLManyToOneType($entityName.id); }
  ;

oneToManyType returns [DMLOneToManyType t]
  : '[' entityName ']' ('.' attributeName)?
    { $t = new DMLOneToManyType($entityName.id, DMLParse.attributeName($attributeName.ctx)); }
  ;

manyToManyType returns [DMLManyToManyType t]
  : '[' a=entityName ']' ('.' attributeName)? '(' l=entityName ')'
    { $t = new DMLManyToManyType($a.id, DMLParse.attributeName($attributeName.ctx), $l.id); }
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
