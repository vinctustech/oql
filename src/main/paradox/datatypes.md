Datatypes
=========

A datatype, in OQL, is distinct from the notion of an attribute type.  Specifically, datatypes are a subclass of attribute type.  Any given OQL datatype is guaranteed to have the same meaning across supported engines that implement the given datatype in some way.

Text
----

In OQL, there is currently a single datatype for representing character string or textual data: `text`.

The `text` datatype corresponds to the PostgreSQL `TEXT` datatype and materializes as a TypeScript `string` primitive or a Scala `String` type object.

Numeric
-------

There are four different categories of numerical data currently in OQL.

### Small integers

These are integers in the range of approximately -2.1 billion to +2.1 billion.  In OQL, this is the `integer` datatype, which has two synonyms: `int` and `int4`.

The `integer` datatype corresponds to the PostgreSQL `INTEGER` datatype and materializes as a TypeScript `number` primitive or a Scala `Int` type object.

### Arbitrarily large integers

These are integers that can be of any size.  In OQL, this is the `bigint` datatype.

The `bigint` datatype corresponds to the PostgreSQL `BIGINT` datatype and materializes as a TypeScript `bigint` primitive or a Scala `BitInt` type object.

### Double precision floating point

These are floating point numbers in a standard (IEEE 754) double precision binary representation with 15 significant digits.  In OQL, this is the `float` datatype, which has one synonym: `float8`.

The `float` datatype corresponds to the PostgreSQL `DOUBLE PRECISION` datatype and materializes as a TypeScript `number` primitive or a Scala `Double` type object.

### Arbitrary precision

These are arbitrarily precise floating point numbers stored in a decimal representation.  In OQL, this is the `decimal(p, s)` parametric datatype, where *p* is the precision giving the number of significant digits, and *s* is the scale giving the number of digits after the decimal point.

The `decimal(p, s)` datatype corresponds to the PostgreSQL `NUMERIC(p, s)` parametric datatype and materializes as a `big.js` (JavaScript library) `Big` type object or a Scala `BigDecimal` type object.

Boolean
-------

The boolean type has three values: `TRUE`, `FALSE`, and `NULL` (representing the unknown state). 

In OQL, this is the `boolean` datatype, which has one synonym: `bool`.

The `boolean` datatype corresponds to the PostgreSQL `BOOLEAN` datatype and materializes as a TypeScript `boolean` primitive or a Scala `Boolean` type object.

UUID
----

Universally Unique Identifiers have type `uuid` which corresponds to the PostgreSQL `UUID` datatype and materializes as a TypeScript `string` primitive or a Scala `String` type object.

Time and Date
-------------

