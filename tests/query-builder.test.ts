import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, type OQL } from './setup.ts'

describe('OQL QueryBuilder', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL()
  })

  after(() => {
    oql.close?.()
  })

  describe('query', () => {
    it('should query from entity', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name }')
        .getMany()
      assert.ok(Array.isArray(users))
      assert.ok(users.length >= 3) // At least seed data
    })

    it('should chain query with filter', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name } [active = true]')
        .getMany()
      assert.ok(Array.isArray(users))
    })
  })

  describe('order', () => {
    it('should order ascending', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name } [name IN ("Alice", "Bob", "Charlie")]')
        .order('name', 'ASC')
        .getMany()
      assert.strictEqual(users.length, 3)
      // First alphabetically should be Alice (from seed data)
      assert.strictEqual(users[0].name, 'Alice')
    })

    it('should order descending', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name } [name IN ("Alice", "Bob", "Charlie")]')
        .order('name', 'DESC')
        .getMany()
      assert.ok(users.length > 0)
      assert.strictEqual(users[0].name, 'Charlie')
    })
  })

  describe('limit and offset', () => {
    it('should limit results', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name }')
        .order('id', 'ASC')
        .limit(2)
        .getMany()
      assert.strictEqual(users.length, 2)
    })

    it('should offset results', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name }')
        .order('id', 'ASC')
        .offset(1)
        .limit(2)
        .getMany()
      assert.strictEqual(users.length, 2)
      assert.strictEqual(users[0].id, 2) // Skipped id=1
    })
  })

  describe('getOne', () => {
    it('should return single result', async () => {
      const user = await oql.queryBuilder()
        .query('users { id name } [id = 1]')
        .getOne()
      assert.ok(user)
      assert.strictEqual(user.id, 1)
    })

    it('should return undefined when no match', async () => {
      const user = await oql.queryBuilder()
        .query('users { id } [id = -999]')
        .getOne()
      assert.strictEqual(user, undefined)
    })
  })

  describe('getCount', () => {
    it('should return count', async () => {
      const count = await oql.queryBuilder()
        .query('users')
        .getCount()
      assert.ok(count >= 3) // At least seed data
    })

    it('should return filtered count', async () => {
      const count = await oql.queryBuilder()
        .query('users [active = true]')
        .getCount()
      assert.ok(count >= 2)
    })
  })

  describe('select', () => {
    it('should add a WHERE condition', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name }')
        .select('id = 1')
        .getMany()
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'Alice')
    })

    it('should chain multiple select calls with AND', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name }')
        .select('id IN (1,2,3)')
        .select('active = true')
        .getMany()
      const ids = users.map((u: any) => u.id).sort((a: number, b: number) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should combine with existing WHERE in query', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name } [id IN (1,2,3)]')
        .select('active = true')
        .getMany()
      const ids = users.map((u: any) => u.id).sort((a: number, b: number) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should support parameters', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name }')
        .select('name = :name', { name: 'Alice' })
        .getMany()
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'Alice')
    })
  })

  describe('cond', () => {
    it('should apply chained operations when condition is truthy', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name } [id IN (1,2,3)]')
        .cond(true)
        .select('active = true')
        .getMany()
      const ids = users.map((u: any) => u.id).sort((a: number, b: number) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should skip chained operations when condition is falsy', async () => {
      const users = await oql.queryBuilder()
        .query('users { id name } [id IN (1,2,3)]')
        .cond(false)
        .select('active = true')
        .getMany()
      // select was skipped, so all 3 users returned
      const ids = users.map((u: any) => u.id).sort((a: number, b: number) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })

    it('should skip on null', async () => {
      const users = await oql.queryBuilder()
        .query('users { id } [id IN (1,2,3)]')
        .cond(null)
        .select('id = 1')
        .getMany()
      assert.strictEqual(users.length, 3)
    })

    it('should skip on empty string', async () => {
      const users = await oql.queryBuilder()
        .query('users { id } [id IN (1,2,3)]')
        .cond('')
        .select('id = 1')
        .getMany()
      assert.strictEqual(users.length, 3)
    })

    it('should apply on non-empty string', async () => {
      const users = await oql.queryBuilder()
        .query('users { id } [id IN (1,2,3)]')
        .cond('active')
        .select('id = 1')
        .getMany()
      assert.strictEqual(users.length, 1)
    })
  })
})
