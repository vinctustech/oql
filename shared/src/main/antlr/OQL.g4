grammar OQL;

@header {
  package com.vinctus.oql2;

  import scala.collection.mutable.ListBuffer;
  import scala.collection.mutable.Buffer;
  import scala.Some;
}

query returns [OQLQuery q]
  : entityName /*project select? group? order? restrict?*/ EOF
    { $q = new OQLQuery($entityName.id, null); }//$project.ps.toList()
  ;

project returns [Buffer<OQLProject> ps]
  : '{' '*' subtracts attributeProjects '}'
    { $ps = $subtracts.ps.prepend(StarOQLProject$.MODULE$).appendAll($attributeProjects.ps); }
  | '{' '*' subtracts '}'
    { $ps = $subtracts.ps.prepend(StarOQLProject$.MODULE$); }
  | '{' attributeProjects '}'
    { $ps = $attributeProjects.ps; }
  | /* empty (equivalent to '{' '*' '}') */
    { $ps = new ListBuffer<OQLProject>().addOne(StarOQLProject$.MODULE$); }
  ;

subtracts returns [ListBuffer<OQLProject> ps]
  : l=subtracts '-' attributeName
    { $ps = $l.ps.addOne(new SubtractOQLProject($attributeName.id)); }
  | /* empty */
    { $ps = new ListBuffer<OQLProject>(); }
  ;

attributeProjects returns [ListBuffer<OQLProject> ps]
  : l=attributeProjects attributeProject
    { $ps = $l.ps.addOne($attributeProject.p); }
  | attributeProject
    { $ps = new ListBuffer<OQLProject>().addOne($attributeProject.p); }
  ;

attributeProject returns [OQLProject p]
  : label? applyExpression
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), $applyExpression.e); }
  | label? COUNT '(' '*' ')'
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), new ApplyOQLExpression(new Ident($COUNT.text, $COUNT.line, $COUNT.pos), OQLParse.star())); }
  | label '(' expression ')'
    { $p = new ExpressionOQLProject(new Some($label.id), $expression.e); }
  | label? qualifiedAttributeName
    { $p = new ExpressionOQLProject(OQLParse.label($label.ctx), $qualifiedAttributeName.e); }
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
    { $e = new InfixOQLExpression($l.e, $o.text, $multiplicative.e); }
  | multiplicative
    { $e = $multiplicative.e; }
  ;

multiplicative returns [OQLExpression e]
  : l=multiplicative o=('*' | '/') primary
    { $e = new InfixOQLExpression($l.e, $o.text, $primary.e); }
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
  | qualifiedAttributeName
    { $e = $qualifiedAttributeName.e; }
  | reference
    { $e = $reference.e; }
  | '-' primary
    { $e = new PrefixOQLExpression("-", $primary.e); }
  | '(' expression ')'
    { $e = $expression.e; }
  ;

logicalPrimary returns [OQLExpression e]
  : b=('TRUE' | 'FALSE')
    { $e = new BooleanOQLExpression($b.text, new Position($b.line, $b.pos)); }
  | parameter
    { $e = $parameter.e; }
  | qualifiedAttributeName
    { $e = $qualifiedAttributeName.e; }
  | '(' logicalExpression ')'
    { $e = $logicalExpression.e; }
  ;

qualifiedAttributeName returns [OQLExpression e]
  : identifiers
    { $e = new AttributeOQLExpression($identifiers.ids.toList()); }
  ;

identifiers returns [ListBuffer<Ident> ids]
  : l=identifiers '.' identifier
    { $ids = $l.ids.addOne($identifier.id); }
  | identifier
    { $ids = new ListBuffer<Ident>().addOne($identifier.id); }
  ;

reference returns [OQLExpression e]
  : '&' identifiers
    { $e = new ReferenceOQLExpression($identifiers.ids.toList()); }
  ;

parameter returns [OQLExpression e]
  : ':' identifier
    { $e = new ParameterOQLExpression($identifier.id); }
  ;

expressions returns [ListBuffer<OQLExpression> es]
  : l=expressions ',' expression
    { $es = $l.es.addOne($expression.e); }
  | expression
    { $es = new ListBuffer<OQLExpression>().addOne($expression.e); }
  | // empty
    { $es = new ListBuffer<OQLExpression>(); }
  ;

select returns [OQLExpression e]
  : '[' logicalExpression ']'
    { $e = $logicalExpression.e; }
  ;

logicalExpression returns [OQLExpression e]
  : orExpression
    { $e = $orExpression.e; }
  ;

orExpression returns [OQLExpression e]
  : l=orExpression o='OR' r=andExpression
    { $e = new InfixOQLExpression($l.e, $o.text, $r.e); }
  | ex=andExpression
    { $e = $ex.e; }
  ;

andExpression returns [OQLExpression e]
  : l=andExpression o='AND' r=notExpression
    { $e = new InfixOQLExpression($l.e, $o.text, $r.e); }
  | ex=notExpression
    { $e = $ex.e; }
  ;

notExpression returns [OQLExpression e]
  : o='NOT' r=comparisonExpression
    { $e = new PrefixOQLExpression($o.text, $r.e); }
  | ex=comparisonExpression
    { $e = $ex.e; }
  ;

comparisonExpression returns [OQLExpression e]
  : l=expression o=comparison r=expression
    { $e = new InfixOQLExpression($l.e, $o.text, $r.e); }
  | exp=expression between l=expression 'AND' u=expression
    { $e = new BetweenOQLExpression($exp.e, $between.text, $l.e, $u.e); }
  | expression isNull
    { $e = new PostfixOQLExpression($expression.e, $isNull.text); }
//  | expression in expressions
//  | expression in '(' query ')'
//  | 'EXISTS' '(' query ')'
  | ex=logicalPrimary
    { $e = $ex.e; }
  ;

in
  : 'NOT'? 'IN'
  ;

comparison
  : '<=' | '>=' | '<' | '>' | '=' | '!=' | 'NOT'? ('LIKE' | 'ILIKE')
  ;

between
  : 'NOT'? 'BETWEEN'
  ;

isNull
  : 'IS' 'NULL' | 'IS' 'NOT' 'NULL'
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
