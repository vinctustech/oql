grammar DML;

@header {
  package com.vinctus.oql2;
}

model
  : entity+
  ;

entity
  : 'entity' entityName ('(' alias ')')? '{' attribute+ '}'
  ;

attribute
  : pk? attributeName ('(' alias ')')? ':' attributeType required?
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

manyToOneType
  : entityName
  ;

oneToManyType
  : '[' entityName ('.' attributeName)? ']'
  ;

alias
  : Ident
  ;

entityName
  : Ident
  ;

attributeName
  : Ident
  ;

Ident
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
