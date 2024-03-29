Syntax
======

The following syntax (or railroad) diagrams of both the data modeling and query language provide a clear description of those languages.  The syntax for `JSON` ([JSON syntax](https://www.json.org/json-en.html)) has been omitted.

### Data Modeling Language

An "Entity-Relationship" style language is used to describe the database.  Only the portions of the database for which OQL is being used need to be described.

#### Data Modeling Grammar Rules

*Model*
: The complete data model is a series of declarations. Currently, only `entity` and `enum` (enumeration) declarations are supported.
: ![Model](.../dml-diagram/Model.png)

*Enum*
: Enums (enumerations) correspond to enum types in the database. An enum comprises an identifier matching the name of the corresponding enum type, and an ordered list of text values (not command separated) known as labels.
: ![Entity](.../dml-diagram/Enum.png)

*Label*
: Enum labels look like single quoted strings (so that you can have spaces in them).
: ![Entity](.../dml-diagram/Label.png)

*Entity*
: Entities correspond to tables in the database. An entity comprises an identifier matching the name of the corresponding table, which can be given an alias, and a series of attributes.
: ![Entity](.../dml-diagram/Entity.png)

*Identifier*
: An identifier can have letters, digits, underscores, and dollar signs, but may not begin with a digit.
: ![Identifier](.../dml-diagram/Identifier.png)

*Alias*
: Aliases are identifiers.

*Attribute*
: Attributes correspond to columns in a table, if they have a data type, or a simple entity type (i.e., just the name of an entity).  An attribute that begins with an asterisk is the primary key.  The name of an attribute can be aliased.  An attribute that ends with an exclamation mark can't be null or excluded when inserting.
: ![Attribute](.../dml-diagram/Attribute.png)

*Type*
: There are several kinds of attribute types:
    - `DataType` corresponding to a database data type. These attributes correspond to table columns that are not foreign keys.
    - `EnumName` corresponding to a database enum type.
    - `EntityName` representing the many-to-one relationship (from the point of view of the current entity). This is the entity relationship type that corresponds to a table column that is a foreign key.
    - `[ EntityName ]` or `[ EntityName ] . AttributeName` representing the one-to-many relationship (from the point of view of the current entity). This is the array type that represents all the entities (rows) that are referencing the current entity.
    - `[ EntityName ] ( EntityName )` or `[ EntityName ] . AttributeName ( EntityName )` representing the many-to-many relationship.  The second `EntityName` in parentheses refers to the junction or link entity (table).
    - `< EntityName >` or `< EntityName > . AttributeName` representing the one-to-one relationship (the current entity is the one being referenced by a foreign key).
: ![Type](.../dml-diagram/Type.png)

*DataType*
: Any one of the basic data types that are commonly support by database systems. Any group of datatypes that begin with the same spelling (beyond the first one or two letters) are synonymous (e.g., `bool` and `boolean` refer to the same datatype).

: ![DataType](.../dml-diagram/DataType.png)

*EnumName*
: An EnumName is an identifier that corresponds to the identifier given as the enum name.

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
: This relational operation specifies the structure of the result. If omitted, the result will comprise all datatype attributes (i.e., columns that are not foreign keys) only, in the order in which they were defined in the data model. The fact that relational attributes must be explicitly specified prevents circularity as well as the retrieval of, possibly very large amounts of unneeded data.  The structure of the result is basically a sequence of expressions each with an (often implicit) label.
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
: The order operation contains a series of one or more ordering expressions by which an array result will be ordered.  Database engines differ in how they order results that contain null values.  OQL guarantees a consistent default behaviour across supported engines.  For ascending ordering, nulls are first.  For descending ordering, nulls are last.
: ![order](.../oql-diagram/order.png)

*restrict*
: This is really two operations that tend to go together: offsetting or skipping an initial number of results, and limiting the number of results.  The first integer is the offset (zero if omitted), and the second is the limit (unlimited if omitted).
: ![restrict](.../oql-diagram/restrict.png)

*booleanExpression*
: A booleanExpression is a boolean (true or false) valued expression that may be a disjunction (logical "or") of two or more boolean valued expressions.
: ![booleanExpression](.../oql-diagram/booleanExpression.png)

*andExpression*
: An andExpression is a boolean valued expression that may be a conjunction (logical "and") of two or more boolean valued expressions.
: ![andExpression](.../oql-diagram/andExpression.png)

*notExpression*
: A notExpression allows a boolean valued expression to be negated.
: ![notExpression](.../oql-diagram/notExpression.png)

*booleanPrimary*
: These are the most basic kinds of boolean valued expressions.
: ![booleanPrimary](.../oql-diagram/booleanPrimary.png)

*expression*
: ![expression](.../oql-diagram/expression.png)

*multiplicative*
: ![multiplicative](.../oql-diagram/multiplicative.png)

*primary*
: ![primary](.../oql-diagram/primary.png)

*identifier*
: ![identifier](.../oql-diagram/identifier.png)

*string*
: There are two types of string literals: single quoted and double quoted.  Single quoted strings are delimited by single quotes, and so cannot contain a single quote unless it is escaped.  Double quoted strings are delimited by double quotes, and so cannot contain a double quote unless it is escaped.  Strings can have the following characters and escape sequences:
    - any unicode character except for: characters in the range \x00-\x1F, character \x7F
    - backslashes are not allowed unless followed by one of the characters: `\'"bfnrtu`
    - if a backslash is followed by a `u`, then 4 hexadecimal digits must follow after that