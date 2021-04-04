Migration from OQL to OQL2
==========================

OQL2 is basically a drop-in replacement for OQL, however, there were a few poor choices in OQL syntax that would only necessitate a tiny number of changes if corrected. This document only deals with breaking changes that need to be taken note of, not enhancements.

Data Modeling Language (DML)
----------------------------

The breaking changes in the DML are:

- many-to-one: `"[" entityType [ "." attributeName ] "]"` is now `"[" entityType "]" [ "." attributeName ]`
- one-to-one: `"<" entityType [ "." attributeName ] ">"` is now `"<" entityType ">" [ "." attributeName ]`
- many-to-many: `"[" entityType [ "." attributeName ] "]" "(" entityType ")"` is now `"[" entityType "]" [ "." attributeName ] "(" entityType ")"`