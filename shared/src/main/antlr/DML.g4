grammar DML;

@header {
  package com.vinctus.oql2;
}

model: entity+;

entity: 'entity' Ident ('(' Ident ')')? '{' attribute+ '}';

attribute: pk? Ident ('(' Ident ')')? ':' type required?;

pk: '*';

required: '!';

type: primitiveType | entityType;

primitiveType:
  'text' |
  'integer' | 'int' | 'int4' |
  'bool' | 'boolean' |
  'bigint' |
  'decimal' |
  'date' |
  'float' | 'float8' |
  'uuid' |
  'timestamp';

entityType: Ident;

Ident: [A-Za-z_] [A-Za-z0-9_]*;

WHITESPACE: [ \t\r\n]+ -> skip;

COMMENT: '/*' (COMMENT | .)*? '*/' -> skip;

LINE_COMMENT: '//' (~[\r\n])* -> skip;
