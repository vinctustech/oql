# @vinctus/oql-petradb

In-memory PetraDB backend for OQL (Object Query Language). Zero external dependencies - no database server required.

## Install

```bash
npm install @vinctus/oql-petradb
```

## Usage

```typescript
import { OQL_PETRADB } from '@vinctus/oql-petradb'

const oql = new OQL_PETRADB(
  dataModel,  // OQL data model string
  storage,    // optional: "memory" (default), "persistent", or "text"
  path,       // optional: file path for persistent/text storage
)

// Seed data with raw SQL
await oql.rawMulti(`
  INSERT INTO users (id, name, email) VALUES (1, 'Alice', 'alice@example.com');
  INSERT INTO users (id, name, email) VALUES (2, 'Bob', 'bob@example.com');
`)

// Query (same API as @vinctus/oql-pg)
const users = await oql.queryMany('user {id name email}')
const user = await oql.queryOne('user [id = :id]', { id: 1 })
const count = await oql.count('user')

// Query builder
const results = await oql.queryBuilder()
  .query('user {id name}')
  .select('name ILIKE :search', { search: '%ali%' })
  .order('name', 'ASC')
  .getMany()

// Mutations
const mutation = oql.entity('user')
await mutation.insert({ name: 'Charlie', email: 'charlie@example.com' })
await mutation.update(id, { name: 'Charles' })
await mutation.delete(id)

// Raw SQL
const rows = await oql.raw('SELECT * FROM users WHERE id = $1', [1])
```

## Storage modes

| Mode | Description |
|------|-------------|
| `"memory"` | In-memory only (default). Data is lost when the process exits. |
| `"persistent"` | Binary file storage. Data persists across restarts. |
| `"text"` | SQL-based text file storage. Data persists across restarts. |

## Use cases

- **Unit/integration testing** without a database server
- **Local development** with fast startup
- **CI pipelines** with zero infrastructure dependencies

## See also

- [`@vinctus/oql-pg`](https://www.npmjs.com/package/@vinctus/oql-pg) - PostgreSQL backend for production use
- [OQL documentation](https://vinctustech.github.io/oql)

## License

ISC
