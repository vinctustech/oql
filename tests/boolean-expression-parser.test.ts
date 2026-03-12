import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL_PG as OQL } from '@vinctus/oql-pg'
import { createOQL } from './setup.ts'

describe('boolean expression parser - standalone non-comparison expressions', () => {
  let oql: OQL

  before(() => {
    oql = createOQL()
  })

  after(() => {
    oql.close()
  })

  // All of these tests demonstrate valid boolean expressions that the old parser
  // rejected because booleanPrimary only fell back to qualifiedAttributeExpression
  // and booleanLiteral — it did not include applyExpression, caseExpression,
  // or castExpression as standalone boolean values.

  describe('function call as standalone boolean', () => {
    it('should accept a function call as a boolean condition', async () => {
      // coalesce(active, true) returns the first non-null: active for all 3 users
      // Alice(true), Bob(true), Charlie(false) → 2 rows where result is truthy
      const users = await oql.queryMany('users { id name } [coalesce(active, true) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should accept NOT function_call()', async () => {
      // NOT coalesce(active, false): Charlie has active=false, coalesce(false,false)=false, NOT false=true
      const users = await oql.queryMany('users { id name } [NOT coalesce(active, false) AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'Charlie')
    })

    it('should accept function call on one side of OR', async () => {
      // id = 999 matches nobody; coalesce(active, true) is truthy for Alice and Bob
      const users = await oql.queryMany('users { id name } [id = 999 OR coalesce(active, true) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should accept function call on one side of AND', async () => {
      // id > 0 AND coalesce(active, true) AND id IN (1,2,3): active is truthy for Alice and Bob
      const users = await oql.queryMany('users { id name } [id > 0 AND coalesce(active, true) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  describe('CASE expression as standalone boolean', () => {
    it('should accept CASE returning boolean as a condition', async () => {
      // CASE WHEN id = 1 THEN true ELSE false END → only Alice
      const users = await oql.queryMany('users { id name } [CASE WHEN id = 1 THEN true ELSE false END]')
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })
  })

  describe('cast expression as standalone boolean', () => {
    it('should accept a cast to boolean as a condition', async () => {
      // active::boolean — Alice and Bob are active
      const users = await oql.queryMany('users { id name } [active::boolean AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  describe('parenthesized expression in boolean context', () => {
    it('should accept a parenthesized function call as boolean', async () => {
      // (coalesce(active, true)) — Alice and Bob are active
      const users = await oql.queryMany('users { id name } [(coalesce(active, true)) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })
})
