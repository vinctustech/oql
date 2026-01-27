import { resetDatabase } from './setup.ts'

// Reset database before all tests
await resetDatabase()
console.log('Database reset complete')
