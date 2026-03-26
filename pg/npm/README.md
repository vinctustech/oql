# @vinctus/oql-pg

PostgreSQL backend for OQL (Object Query Language). Connects to PostgreSQL via [node-pg](https://node-postgres.com/) with connection pooling.

## Install

```bash
npm install @vinctus/oql-pg
```

## Usage

```typescript
import { OQL_PG } from '@vinctus/oql-pg'

const oql = new OQL_PG(
  dataModel,   // OQL data model string
  host,        // PostgreSQL host
  port,        // PostgreSQL port
  database,    // database name
  user,        // username
  password,    // password
  ssl,         // SSL config (boolean or object)
  idleTimeoutMillis, // connection idle timeout
  max,         // max pool size
)

// Query
const users = await oql.queryMany('user {id firstName lastName}')
const user = await oql.queryOne('user [id = :id]', { id: userId })
const count = await oql.count('user [active = true]')

// Query builder
const results = await oql.queryBuilder()
  .query('user {id firstName lastName}')
  .select('active = true')
  .order('lastName', 'ASC')
  .limit(25)
  .getMany()

// Mutations
const mutation = oql.entity('user')
await mutation.insert({ firstName: 'Alice', lastName: 'Smith', active: true })
await mutation.update(id, { active: false })
await mutation.delete(id)

// Raw SQL
const rows = await oql.raw('SELECT * FROM users WHERE id = $1', [userId])

// Cleanup
oql.close()
```

## Migrating from @vinctus/oql

Change your import and constructor:

```diff
- import { OQL } from '@vinctus/oql'
- const oql = new OQL(dm, host, port, database, user, password, ssl, idle, max)
+ import { OQL_PG } from '@vinctus/oql-pg'
+ const oql = new OQL_PG(dm, host, port, database, user, password, ssl, idle, max)
```

The query and mutation APIs are identical.

## See also

- [`@vinctus/oql-petradb`](https://www.npmjs.com/package/@vinctus/oql-petradb) - In-memory PetraDB backend (no PostgreSQL required)
- [OQL documentation](https://vinctustech.github.io/oql)

## License

ISC
