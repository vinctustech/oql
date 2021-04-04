grammar OQL;

@header {
  package com.vinctus.oql2;

  import scala.collection.mutable.ListBuffer;
}

query
  : entityName project select? group? order? restrict?
  ;

project
  : '{' '*' subtracts attributeProjects '}'
  | '{' '*' subtracts '}'
  | '{' attributeProjects '}'
  | /* empty (equivalent to '{' '*' '}') */
  ;

subtracts
  : subtracts '-' attributeName
  | /* empty */
  ;

attributeProjects
  : attributeProjects attributeProject
  | attributeProject
  ;

attributeProject
  : attributeName '('
  | attributeName
  | '&' attributeName
  ;

expression returns [Expression e]
  : additive
    { $e = $additive.e; }
  ;

additive returns [Expression e]
  : l=additive o=('+' | '-') multiplicative
    { $e = new BinaryExpression($l.e, $o.text, $multiplicative.e); }
  | multiplicative
    { $e = $multiplicative.e; }
  ;

multiplicative returns [Expression e]
  : l=multiplicative o=('*' | '/') primary
    { $e = new BinaryExpression($l.e, $o.text, $primary.e); }
  | primary
    { $e = $primary.e; }
  ;

primary returns [Expression e]
  : NUMBER
    { $e = new NumberExpression(Double.parseDouble($NUMBER.text), $NUMBER.line, $NUMBER.pos); }
  | identifier '(' expressions ')'
    { $e = new ApplyExpression($identifier.id, $expressions.es.toList()); }
  | identifier
    { $e = new VariableExpression($identifier.id); }
  | '-' primary
    { $e = new UnaryExpression($primary.e, "-"); }
  | '(' expression ')'
    { $e = $expression.e; }
  ;

expressions returns [scala.collection.mutable.ListBuffer<Expression> es]
  : l=expressions ',' expression
    { $es = $l.es.addOne($expression.e); }
  | expression
    { $es = new scala.collection.mutable.ListBuffer<Expression>().addOne($expression.e); }
  | // empty
    { $es = new scala.collection.mutable.ListBuffer<Expression>(); }
  ;

select
  :
  ;

group
  :
  ;

order
  :
  ;

restrict
  :
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

NUMBER
  : [0-9]+ ('.' [0-9]+)? EXPONENT?
  | '.' [0-9]+ EXPONENT?
  ;

EXPONENT
  : [eE] [+-]? [0-9]+
  ;

IDENTIFIER
  : [A-Za-z_] [A-Za-z0-9_]*
  ;

WS
  : [ \t\r\n]+ -> skip
  ;

COMMENT
  : '/*' (COMMENT | .)*? '*/' -> skip
  ;

LINE_COMMENT
  : '//' (~[\r\n])* -> skip
  ;
