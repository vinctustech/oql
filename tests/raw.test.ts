import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, type OQL } from './setup.ts'

describe('OQL raw SQL', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL()
  })

  after(() => {
    oql.close?.()
  })

  describe('raw', () => {
    it('should execute raw SELECT query', async () => {
      const rows = await oql.raw<{id: number, name: string}>(
        'SELECT id, name FROM users WHERE id IN (1,2,3) ORDER BY id'
      )

      assert.deepStrictEqual(rows, [
        { id: 1, name: 'Alice' },
        { id: 2, name: 'Bob' },
        { id: 3, name: 'Charlie' }
      ])
    })

    it('should execute raw query with parameters', async () => {
      const rows = await oql.raw<{id: number, name: string}>(
        'SELECT id, name FROM users WHERE id = $1',
        [1]
      )

      assert.deepStrictEqual(rows, [{ id: 1, name: 'Alice' }])
    })

    it('should execute raw query with multiple parameters', async () => {
      const rows = await oql.raw<{id: number, name: string}>(
        'SELECT id, name FROM users WHERE active = $1 AND id = $2',
        [true, 1]
      )

      assert.deepStrictEqual(rows, [{ id: 1, name: 'Alice' }])
    })

    it('should return empty array for no matches', async () => {
      const rows = await oql.raw('SELECT id FROM users WHERE id = $1', [-999])
      assert.deepStrictEqual(rows, [])
    })

    it('should execute INSERT and return result', async () => {
      const rows = await oql.raw<{id: number, name: string}>(
        'INSERT INTO mut_users (name, email, active) VALUES ($1, $2, $3) RETURNING id, name',
        ['RawInsertUser', 'raw@example.com', true]
      )

      assert.strictEqual(rows.length, 1)
      assert.strictEqual(typeof rows[0].id, 'number')
      assert.strictEqual(rows[0].name, 'RawInsertUser')

      // Cleanup
      await oql.raw('DELETE FROM mut_users WHERE id = $1', [rows[0].id])
    })

    it('should execute UPDATE and return result', async () => {
      // Insert test record
      const inserted = await oql.raw<{id: number, name: string}>(
        'INSERT INTO mut_users (name, email, active) VALUES ($1, $2, $3) RETURNING id, name',
        ['RawUpdateUser', 'rawupdate@example.com', true]
      )

      assert.strictEqual(inserted.length, 1)
      assert.strictEqual(inserted[0].name, 'RawUpdateUser')

      // Update it
      const updated = await oql.raw<{id: number, name: string, email: string}>(
        'UPDATE mut_users SET name = $1 WHERE id = $2 RETURNING id, name, email',
        ['RawUpdatedUser', inserted[0].id]
      )

      assert.deepStrictEqual(updated, [{
        id: inserted[0].id,
        name: 'RawUpdatedUser',
        email: 'rawupdate@example.com'
      }])

      // Cleanup
      await oql.raw('DELETE FROM mut_users WHERE id = $1', [inserted[0].id])
    })
  })
})
