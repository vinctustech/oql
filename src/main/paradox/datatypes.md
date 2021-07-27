Data types
==========

A data type, in OQL, is distinct from the notion of an attribute type.  Specifically, data types are a subclass of attribute type.  Any given OQL data type is guaranteed to have the same meaning across supported engines that implement the given data type in some way.

Text
----

In OQL, there is currently a single data type for representing character string or textual data: `text`.

The `text` data type corresponds to the PostgreSQL `TEXT` data type and materializes as a TypeScript `string` primitive or a Scala `String` type object.

Numeric
-------

There are four different categories of numerical data currently in OQL.

### Small integers

These are integers in the range of approximately -2.1 billion to +2.1 billion.  In OQL, this is the `integer` data type, which has two synonyms: `int` and `int4`.

The `integer` data type corresponds to the PostgreSQL `INTEGER` data type and materializes as a TypeScript `number` primitive or a Scala `Int` type object.

### Double precision floating point

These are floating point numbers in a standard (IEEE 754) double precision binary representation with 15 significant digits.  In OQL, this is the `float` data type, which has one synonym: `float8`.

The `float` data type corresponds to the PostgreSQL `DOUBLE PRECISION` data type and materializes as a TypeScript `number` primitive or a Scala `Double` type object.

Boolean
-------

The boolean type has three values: `TRUE`, `FALSE`, and `NULL` (representing the unknown state). 

In OQL, this is the `boolean` data type, which has one synonym: `bool`.

The `boolean` data type corresponds to the PostgreSQL `BOOLEAN` data type and materializes as a TypeScript `boolean` primitive or a Scala `Boolean` type object.

UUID
----

Universally Unique Identifiers have type `uuid` which corresponds to the PostgreSQL `UUID` data type and materializes as a TypeScript `string` primitive or a Scala `String` type object.

Time and Date
-------------

There are two data types relating to time and date:

### Timestamps

Timestamps are denoted `timestamp` which corresponds to the PostgreSQL `TIMESTAMP WITHOUT TIME ZONE` data type and materializes as a TypeScript `Date` type object or a Scala `Instant` type object.
