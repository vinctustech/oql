Syntax
======

The following syntax (or railroad) diagrams of both the data modeling and query language provide a clear description of those languages.  The syntax for `json` ([JSON syntax](https://www.json.org/json-en.html)) has been omitted.

### Data Modeling Language

An "Entity-Relationship" style language is used to describe the database.  Only the portions of the database for which OQL is being used need to be described.

#### Data Modeling Grammar Rules

*Model*
: The complete data model is a series of entities.
: ![Model](.../dml-diagram/Model.png)

*Entity*
: Entities correspond to tables in the database. An entity comprises an identifier matching the name of the corresponding table, which can be given an alias, and a series of attributes.
: ![Entity](.../dml-diagram/Entity.png)

*Identifier*
: An identifier can have letters, digits, underscores, and dollar signs, but may not begin with a digit.
: ![Identifier](.../dml-diagram/Identifier.png)

*Alias*
: Aliases are identifiers.

*Attribute*
: Attributes correspond to columns in a table, if they have a datatype, or a simple entity type (i.e., just the name of an entity).  An attribute that begins with an asterisk is the primary key.  The name of an attribute can be aliased.  An attribute that ends with an exclamation mark can't be null or excluded when inserting.
: ![Attribute](.../dml-diagram/Attribute.png)

*Type*
: There are several kinds of attribute types:
    - `DataType` corresponding to a database engine datatype. These attributes correspond to table columns that are not foreign keys.
    - `EntityName` representing the many-to-one relationship (from the point of view of the current entity). This is the entity relationship type that corresponds to a table column that is a foreign key.
    - `[ EntityName ]` or `[ EntityName ] . AttributeName` representing the one-to-many relationship (from the point of view of the current entity). This is the array type that represents all the entities (rows) that are referencing the current entity.
    - `[ EntityName ] ( EntityName )` or `[ EntityName ] . AttributeName ( EntityName )` representing the many-to-many relationship.  The second `EntityName` in parentheses refers to the junction or link entity (table).
    - `< EntityName >` or `< EntityName > . AttributeName` representing the one-to-many relationship (the current entity is the one being referenced by a foreign key).
: ![Type](.../dml-diagram/Type.png)

*DataType*
: Any one of the basic datatypes that are commonly support by database systems. Any group of datatypes that begin with the same spelling (beyond the first one or two letters) are synonymous (e.g., `bool` and `boolean` refer to the same datatype).

: ![DataType](.../dml-diagram/DataType.png)

*EntityName*
: An EntityName is an identifier that corresponds either to the identifier given as the entity name if it wasn't aliased, or to the alias if it was.

*AttributeName*
: An AttributeName is an identifier that corresponds either to the identifier given as the attribute name if it wasn't aliased, or to the alias if it was.

### Query Language

The query language is inspired by GraphQL. In the following grammar, all keywords (double-quoted string literals) are case-insensitive.

#### Query Grammar Rules

*query*
: A data retrieval query expression begins with the name of the entity being queried followed by zero or more optional relational operations:
: ![query](.../oql-diagram/query.png)

*project*
: This relational operation specifies the structure of the results. If omitted, result will comprise all datatype attributes only, in the order in which they were defined in the data model. The structure of the result be basically a sequence of expressions each with an implicit or explicit `label`, namely `attributeProject`.
: ![project](.../oql-diagram/project.png)

*attributeProject*
: The syntax of an attribute within a project is essentially a possibly optional label, followed by an expression, or an inner query if it is a reference to an array type attribute.
: ![attributeProject](.../oql-diagram/attributeProject.png)

*label*
: Results are in the form of objects which can are thought of as a list of property/value pairs. Therefore, in order to build a result, the property names have to be known. A label, when it is given becomes the property name of the associated value.  If omitted, there are simple obvious rules for inferring it.
: ![label](.../oql-diagram/label.png)

*entityName*
: An entityName is an identifier that names an entity (table) or it's alias.

*attributeName*
: An attributeName is an identifier that names a column (attribute) or it's alias.

*applyExpression*
: Function application has conventional syntax (e.g., `SUM(cost)`).  Currently, function names pass through transpilation unchecked.
: ![applyExpression](.../oql-diagram/applyExpression.png)

*attributeExpression*
: An attributeExpression is an identifier that names an attribute, whether it's a column or not, or it's alias.

*qualifiedAttributeExpression*
: This is a reference to an attribute that essentially traverses or dereferences a foreign key.
: ![qualifiedAttributeExpression](.../oql-diagram/qualifiedAttributeExpression.png)

*select*
: The select relational operation contains a boolean row selection or filter expression.  If omitted, all rows will be retrieved.The row selection condition must be a boolean valued expression.
: ![select](.../oql-diagram/select.png)

*group*
: The group operation contains a series of one or more grouping expressions by which an array result will be grouped.
: ![group](.../oql-diagram/group.png)

*order*
: The order operation contains a series of one or more ordering expressions by which an array result will be ordered.
: ![order](.../oql-diagram/order.png)

- `restrict` contains one or two integers giving the offset and limit. If omitted, the offset is zero.
