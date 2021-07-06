Syntax
======

The following syntax (or railroad) diagrams of both the data modeling and query language provide a clear description of those languages.  The syntax for `json` ([JSON syntax](https://www.json.org/json-en.html)) has been omitted.

### Data Modeling Language

An "Entity-Relationship" style language is used to describe the database.  Only the portions of the database for which OQL is being used need to be described.

#### Grammar Rules

Model
: The complete data model is a series of entities.
: ![Model](.../diagram/Model.png)

Entity
: Entities correspond to tables in the database. An entity comprises an identifier matching the name of the corresponding table, which can be given an alias, and a series of attributes.
: ![Entity](.../diagram/Entity.png)

Identifier
: An identifier can have letters, digits, underscores, and dollar signs, but may not begin with a digit.
: ![Identifier](.../diagram/Identifier.png)
