Usage
=====

```typescript
import { OQL } from '@vinctus/oql'

const oql = new OQL(<data model>, <host>, <port>, <database>, <user>, <password>, <ssl>, <idleTimeoutMillis>, <max>)

oql.queryMany(<query>).then((result: any) => <handle result> )
```

`<host>`, `<port>`, `<database>`, `<user>`, `<password>`, `<ssl>`, `<idleTimeoutMillis>`, and `<max>` are the connection pool (`PoolConfig`) parameters for the Postgres database you are querying.

`<data model>` describes the parts of the database available for querying.  It's not necessary to describe every field of every table in the database, only what is being retrieved with *OQL*.  However, primary keys of tables that are being queried should always be included, even if you're not interested in retrieving the primary keys themselves.

`<query>` is the OQL query string.

`<handle result>` is your result array handling code.  The `result` object will be predictably structured according to the query.
