import { backend, resetDatabase } from './setup.ts'

if (backend === 'pg') {
  // Prevent node-pg from auto-parsing JSON/JSONB into JS objects — OQL expects raw strings
  const pg = await import('pg')
  pg.default.types.setTypeParser(114, val => val)   // json OID
  pg.default.types.setTypeParser(3802, val => val)  // jsonb OID
}

// Reset database before all tests
await resetDatabase()
console.log(`Database reset complete (backend: ${backend})`)
