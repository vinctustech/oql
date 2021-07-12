Introduction
============

Overview
--------

*OQL* is a language for querying a relational database. The query syntax draws inspiration from GraphQL and is similar, but with many differences.  Some capabilities missing from GraphQL have been added, and some capabilities found in GraphQL have a different syntax.  We believe that much of conventional SQL syntax is still preferable to creating a completely new syntax for every single feature of the query language (i.e., conventional syntax wherever possible).  However, whereas SQL can be seen as a "low level" relational query language, OQL takes a higher level Entity-Relationship (ER) Model view of the database.

The name "OQL" refers to both a software library, and a query language implemented within that library.  The OQL library provides support for data retrieval (via the OQL query language) as well as a query builder for constructing queries in programmatically.  There are also class methods for performing all kinds of mutations, including mutations that support the ER view of the database.  Furthermore, query and mutation operations all abide by the supplied ER data model.

Some features of OQL include:

- the query language is similar to [GraphQL](https://graphql.org/) in that query results have exactly the structure requested in the query (i.e., you get what you ask for)
- uses a very simple [Entity-Relationship Model](https://en.wikipedia.org/wiki/Entity%E2%80%93relationship_model) description of the database
- works with the [PostgreSQL](https://www.postgresql.org/) database system
- designed to work with existing databases without having to change the database at all

Relationships
-------------

OQL recognizes all the various kinds of entity relationships, and provides a way to explicitly define them:

One-to-Many
: The *one-to-many* relationship means that an object of some entity *A* can relate to any number of objects of some entity *B*. It is possible for *A* and *B* to be the same entity: entities can be in a self relationship. Furthermore, that self relationship can be indirect with intermediate entities.  The one-to-many relationship is just the flip version of many-to-one: if *A* is one-to-many in how it relates to *B*, then *B* is many-to-one in how it relates to *A*.

Many-to-one
: The *many-to-one* relationship means that any number of objects of some entity *A* can relate to a single object of some entity *B*.

Many-to-many
: The *many-to-many* relationship means that any number of objects of some entity *A* can relate to any number of objects of some entity *B*.  In OQL, this is accomplished by both defining that the relationship exists and defining a junction or link entity to facilitate the links between *A* and *B*.

One-to-one
: The *one-to-one* relationship that an object of some entity *A* can relate to a single object of some entity *B* and no more.