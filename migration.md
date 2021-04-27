Migration from OQL to OQL2
==========================

OQL2 is basically a drop-in replacement for OQL, however, there were a few poor choices in OQL syntax that would necessitate only a tiny number of changes if corrected. This document only deals with breaking changes that need to be taken note of. Enhancements to OQL are not discussed here.

Data Modeling Language (DML)
----------------------------

The breaking changes in the DML are:

- many-to-one: `"[" entityType [ "." attributeName ] "]"` is now `"[" entityType "]" [ "." attributeName ]`
- one-to-one: `"<" entityType [ "." attributeName ] ">"` is now `"<" entityType ">" [ "." attributeName ]`
- many-to-many: `"[" entityType [ "." attributeName ] "]" "(" entityType ")"` is now `"[" entityType "]" [ "." attributeName ] "(" entityType ")"`

Query Language (OQL)
--------------------

The breaking changes in the query language are:

- in projections, "subtracts" (`"-" attributeName`) can only be written right after a `*`, which has to be the first element of the projection if it occurs
- keywords must be written in uppercase
- the default for `count(*)` is now `count`

API
---

The query builder has some differences due to the fact that query parameters are now recognized by the parser. This means that query parameters appear the same way the query (e.g `:id`) but the actual parameter are supplied when the query is executed as an argument to the `get___()` methods.
