Overview
========

*OQL* is a language for querying a relational database. The query syntax draws inspiration from GraphQL and is similar, but with many differences.  Some capabilities missing from GraphQL have been added, and some capabilities found in GraphQL have a different syntax.  We believe that much of conventional SQL syntax is still preferable to creating a completely new syntax for every single feature of the query language (i.e., conventional syntax wherever possible).  However, whereas SQL can be seen as a "low level" relational query language, OQL takes a higher level Entity-Relationship (ER) Model view of the database.

The name "OQL" refers to both a software library, and a query language implemented within that library.  The OQL library provides support for data retrieval (via the OQL query language) as well as a query builder for constructing queries in programmatically.  There are also class methods for performing all kinds of mutations, including mutations that support the ER view of the database.  Furthermore, query and mutation operations all abide by the supplied ER data model.

Some features of the OQL language include:

- similar to [GraphQL](https://graphql.org/) in that query results have exactly the structure requested in the query (i.e., you get what you ask for)
- uses a very simple [Entity-Relationship Model](https://en.wikipedia.org/wiki/Entity%E2%80%93relationship_model) description of the database
- works with the [PostgreSQL](https://www.postgresql.org/) database system
- designed to work with existing databases without having to change the database at all
