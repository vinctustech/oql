Syntax
======

The following syntax (or railroad) diagrams of both the data modeling and query language provide a clear description of those languages.  The syntax for `json` ([JSON syntax](https://www.json.org/json-en.html)) has been omitted.

### Data Modeling Language

An "Entity-Relationship" style language is used to describe the database.  Only the portions of the database for which OQL is being used need to be described.

#### Data Modeling Grammar Rules

Model
: The complete data model is a series of entities.
: ![Model](.../diagram/Model.png)

Entity
: Entities correspond to tables in the database. An entity comprises an identifier matching the name of the corresponding table, which can be given an alias, and a series of attributes.
: ![Entity](.../diagram/Entity.png)

Identifier
: An identifier can have letters, digits, underscores, and dollar signs, but may not begin with a digit.
: ![Identifier](.../diagram/Identifier.png)

Alias
: Aliases are identifiers.

Attribute
: Attributes correspond to columns in a table, if they have a datatype, or a simple entity type (i.e., just the name of an entity).  An attribute that begins with an asterisk is the primary key.  The name of an attribute can be aliased.  An attribute that ends with an exclamation mark can't be null or excluded when inserting.
: ![Attribute](.../diagram/Attribute.png)

Type
: There are several kinds of attribute types:

    Datatype
    : Corresponds to a databased engine datatype. These attributes correspond to table columns that are not foreign keys.

    Many-to-one
    : This is the entity relationship type that corresponds to a column.

    One-to-many
    : This is the array entity relationship type that represents all the entities (rows) that are referencing any given entity. 

    Many-to-many
    : the many-to-many relationship

    One-to-many
    : the one-to-many relationship

: ![Type](.../diagram/Type.png)

DataType
: Any one of the basic datatypes that are commonly support by database systems.
: ![DataType](.../diagram/DataType.png)

EntityName
: An EntityName is an identifier that corresponds either to the identifier given as the entity name if it wasn't aliased, or to the alias if it was.

AttributeName
: An AttributeName is an identifier that corresponds either to the identifier given as the attribute name if it wasn't aliased, or to the alias if it was.

### Query Language

The query language is inspired by GraphQL. In the following grammar, all keywords (double-quoted string literals) are case-insensitive.

#### Query Grammar Rules

