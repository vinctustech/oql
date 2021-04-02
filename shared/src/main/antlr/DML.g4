grammar DML;

@header {
  package com.vinctus.oql2;
}

model: entity+;

entity: 'entity' Ident '{' '}';

Ident: [A-Za-z_] [A-Za-z0-9_]*;

WP: [ \t\r\n]+ -> skip;
