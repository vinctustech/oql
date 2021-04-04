grammar OQL;

@header {
  package com.vinctus.oql2;

  import scala.collection.mutable.ListBuffer;
  import scala.Some;
}

query returns [OQLQuery q]
  : entityName project select? group? order? restrict?
  ;

project
  : '{' '*' subtracts attributeProjects '}'
  | '{' '*' subtracts '}'
  | '{' attributeProjects '}'
  | '.' attributeName
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

attributeProject returns [OQLProject p]
  : label? applyExpression
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), $applyExpression.e); }
  | label? COUNT '(' '*' ')'
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), new ApplyOQLExpression(new Ident($COUNT.text, $COUNT.line, $COUNT.pos), OQLParse.star())); }
  | label '(' expression ')'
    { $p = new ExpressionOQLProject(new Some($label.id), $expression.e); }
  | label? variable
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), $variable.e); }
  | label? reference
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), $reference.e); }
  | label? parameter
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), $parameter.e); }
  | label? query
    { $p = new QueryOQLProject(OQLParse.label($label.ctx), $query.q); }
  ;

label returns [Ident id]
  : identifier ':'
    { $id = $identifier.id; }
  ;

expression returns [OQLExpression e]
  : additive
    { $e = $additive.e; }
  ;

additive returns [OQLExpression e]
  : l=additive o=('+' | '-') multiplicative
    { $e = new BinaryOQLExpression($l.e, $o.text, $multiplicative.e); }
  | multiplicative
    { $e = $multiplicative.e; }
  ;

multiplicative returns [OQLExpression e]
  : l=multiplicative o=('*' | '/') primary
    { $e = new BinaryOQLExpression($l.e, $o.text, $primary.e); }
  | primary
    { $e = $primary.e; }
  ;

applyExpression returns [OQLExpression e]
  : identifier '(' expressions ')'
    { $e = new ApplyOQLExpression($identifier.id, $expressions.es.toList()); }
  ;

primary returns [OQLExpression e]
  : NUMBER
    { $e = new NumberOQLExpression(Double.parseDouble($NUMBER.text), new Position($NUMBER.line, $NUMBER.pos)); }
  | b=('TRUE' | 'FALSE')
    { $e = new BooleanOQLExpression($b.text, new Position($b.line, $b.pos)); }
  | applyExpression
    { $e = $applyExpression.e; }
  | parameter
    { $e = $parameter.e; }
  | variable
    { $e = $variable.e; }
  | reference
    { $e = $reference.e; }
  | '-' primary
    { $e = new UnaryOQLExpression($primary.e, "-"); }
  | '(' expression ')'
    { $e = $expression.e; }
  ;

logicalPrimary returns [OQLExpression e]
  : b=('TRUE' | 'FALSE')
    { $e = new BooleanOQLExpression($b.text, new Position($b.line, $b.pos)); }
  | parameter
    { $e = $parameter.e; }
  | variable
    { $e = $variable.e; }
  | '(' logicalExpression ')'
    { $e = $logicalExpression.e; }
  ;

variable returns [OQLExpression e]
  : identifier
    { $e = new VariableOQLExpression($identifier.id); }
  ;

reference returns [OQLExpression e]
  : '&' attributeName
    { $e = new ReferenceOQLExpression($attributeName.id); }
  ;

parameter returns [OQLExpression e]
  : ':' identifier
    { $e = new ParameterOQLExpression($identifier.id); }
  ;

expressions returns [scala.collection.mutable.ListBuffer<OQLExpression> es]
  : l=expressions ',' expression
    { $es = $l.es.addOne($expression.e); }
  | expression
    { $es = new scala.collection.mutable.ListBuffer<OQLExpression>().addOne($expression.e); }
  | // empty
    { $es = new scala.collection.mutable.ListBuffer<OQLExpression>(); }
  ;

select
  : '[' logicalExpression ']'
  ;

logicalExpression returns [OQLExpression e]
  : orExpression
  ;

orExpression
  : orExpression 'OR' andExpression
  | andExpression
  ;

andExpression
  : andExpression 'AND' notExpression
  | notExpression
  ;

notExpression
  : 'NOT' comparisonExpression
  | comparisonExpression
  ;

comparisonExpression
  : expression ('<=' | '>=' | '<' | '>' | '=' | '!=' | 'NOT'? ('LIKE' | 'ILIKE')) expression
  | expression 'NOT'? 'BETWEEN' expression 'AND' expression
  | expression ('IS' 'NULL' | 'IS' 'NOT' 'NULL')
  | expression 'NOT'? 'IN' expressions
  | expression 'NOT'? 'IN' '(' query ')'
  | 'EXISTS' '(' query ')'
  | logicalPrimary
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

COUNT
  : [Cc] [Oo] [Uu] [Nn] [Tt]
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
