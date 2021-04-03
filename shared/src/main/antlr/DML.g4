grammar DML;

@header {
  package com.vinctus.oql2;
}

model: entity+;

entity: 'entity' Ident ('(' Ident ')')? '{' attribute+ '}';

attribute: '*'? Ident ('(' Ident ')')?;

Ident: [A-Za-z_] [A-Za-z0-9_]*;

WHITESPACE: [ \t\r\n]+ -> skip;

COMMENT: '/*' (COMMENT | .)*? '*/' -> skip;

LINE_COMMENT: '//' (~[\r\n])* -> skip;
