import pg from 'pg'
import { resetDatabase } from './setup.ts'

// Prevent node-pg from auto-parsing JSON/JSONB into JS objects — OQL expects raw strings
pg.types.setTypeParser(114, val => val)   // json OID
pg.types.setTypeParser(3802, val => val)  // jsonb OID

// Reset database before all tests
await resetDatabase()
console.log('Database reset complete')
