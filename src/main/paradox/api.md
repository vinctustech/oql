API
===

The TypeScript API is documented first here followed by a few notes on the Scala.js API which is very similar.

The `OQL` Class
---------------

These are the methods of the `OQL` class, which are the main methods that you will be using. Brackets around a parameter signify an optional parameter.

### `count(query, [parameters])`

Returns a promise for the number of objects where *query* is the query string written in the [OQL query language](#query-language). If *parameters* is given, each parameter is referenced in the query as `:name` where *name* is the name of the parameter.

For example

```typescript
oql.count('product [price < :max]', {max: 100.00})
```

gets the number of products that are less than $100.

### `entity(name)`

Returns a `Mutation` instance for OQL class instance that it was called on. See [The `Mutation` Class](#the-resource-class) for a method reference.

### `queryBuilder()`

Returns a `QueryBuilder` instance for OQL class instance that it was called on. See [The `QueryBuilder` Class](#the-querybuilder-class) for a method reference.

### `queryOne(query, [parameters])`

Returns a promise for zero or one object where *query* is the query string written in the [OQL query language](#query-language). If *parameters* is given, each parameter is referenced in the query as `:name` where *name* is the name of the parameter.

For example

```typescript
oql.queryOne('user {id name email} [id < :id]', {id: 12345})
```

gets the *id*, *name*, and *email* for user with id 12345.

### `queryMany(query, [parameters])`

Returns a promise for an array of objects where *query* is the query string written in the [OQL query language](#query-language). If *parameters* is given, each parameter is referenced in the query as `:name` where *name* is the name of the parameter.

For example

```typescript
oql.queryMany('product {id name price supplier.name} [price < :max]', {max: 100.00})
```

gets the *id*, *name*, *price* and `supplier.name` for products that are less than $100.

### `raw(sql, [values])`

Perform the raw SQL query and return a promise for the results where *sql* is the query string and *values* are query parameter values.

The `QueryBuilder` Class
------------------------

`QueryBuilder` is used to build up a query step by step. `QueryBuilder` instances are immutable so each method that returns a `QueryBuilder` object is returning a new instance.

### `cond(exp)`

Blocks the next method call in a chain of `QueryBuilder` calls if the condition expression *exp* is falsy.

### `getCount()`

Returns a promise for the number of objects that could be retrieved with `this` query builder.

### `getMany()`

Returns a promise for an array of object specified by `this` query builder.

### `getOne()`

Returns a promise for zero or one object specified by `this` query builder.

### `limit(a)`

Returns a new query builder with a query limit of *a*.

### `offset(a)`

Returns a new query builder with a query offset of *a*.

### `order(attribute, sorting)`

Returns a new query builder with a query ordering on `attribute` with `sorting` direction.

### `project(entity, attributes)`

Returns a new query builder to query *entiry* retrieving *attributes*. This method is a bit more efficient than using `query()` because it avoids parsing the query.

### `query(base_query, [parameters])`

Returns a new query builder with the given *base_query*, which must be a well-formed OQL query.

### `select(selection, [parameters])`

Returns a new query builder with the given *selection*.  If a selection has been given, either using `query()` (with the selection within brackets) or using `select()`, then this selection will be logically AND'ed with the previous one. There or no need to add parentheses to ensure correct order of operations if the selection contains a logical OR, this is done internally.

The `Mutation` Class
--------------------

These are methods that can be called on a resource object.

### `delete(id)`

Returns a promise to delete object with primary key *id*.

### `getMany()`

Returns a promise for all objects of `this` resource.

### `insert(obj)`

Returns a promise to insert *obj* into `this` resource. The promise resolves to an object with the primary key of the inserted object.

### `link(e1, attribute, e2)`

Returns a promise to link object *e1* of `this` resource to object *e2* of the type given for *attribute*.

### `unlink(e1, attribute, e2)`

Returns a promise to unlink object *e1* of `this` resource to object *e2* of the type given for *attribute*.

### `update(e, updates)`

Returns a promise to unlink object *e* of `this` resource according to *updates*.
