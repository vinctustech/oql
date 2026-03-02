import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { createOQL } from './setup.ts'

describe('Type casts', () => {
  let oql: OQL

  before(() => {
    oql = createOQL()
  })

  after(() => {
    oql.close()
  })

  // Decimal/Numeric casts

  describe('numeric cast', () => {
    it('bare numeric should cast to number', async () => {
      const row = await oql.queryOne("users { val: ('3.14'::numeric) } [id = 1]")
      assert.strictEqual(typeof row.val, 'number')
      assert.strictEqual(row.val, 3.14)
    })

    it('numeric(p,s) should cast with precision and scale', async () => {
      const row = await oql.queryOne("users { val: ('3.14159'::numeric(10,2)) } [id = 1]")
      assert.strictEqual(typeof row.val, 'number')
      assert.strictEqual(row.val, 3.14)
    })

    it('bare decimal should cast to number', async () => {
      const row = await oql.queryOne("users { val: ('2.718'::decimal) } [id = 1]")
      assert.strictEqual(typeof row.val, 'number')
      assert.strictEqual(row.val, 2.718)
    })

    it('decimal(p,s) should cast with precision and scale', async () => {
      const row = await oql.queryOne("users { val: ('2.71828'::decimal(10,3)) } [id = 1]")
      assert.strictEqual(typeof row.val, 'number')
      assert.strictEqual(row.val, 2.718)
    })
  })

  describe('numeric cast in WHERE', () => {
    it('should compare numeric cast values', async () => {
      const rows = await oql.queryMany("users { id } ['3.14'::numeric > 3]")
      assert.ok(rows.length > 0)
    })
  })

  // Array type casts

  describe('integer[] cast', () => {
    it('should cast string to integer array', async () => {
      const row = await oql.queryOne("users { val: ('{1,2,3}'::integer[]) } [id = 1]")
      assert.deepStrictEqual(row.val, [1, 2, 3])
    })
  })

  describe('text[] cast', () => {
    it('should cast string to text array', async () => {
      const row = await oql.queryOne("users { val: ('{hello,world}'::text[]) } [id = 1]")
      assert.deepStrictEqual(row.val, ['hello', 'world'])
    })
  })

  describe('array cast in WHERE', () => {
    it('should use array cast with ANY', async () => {
      const rows = await oql.queryMany("users { id name } [id = ANY('{1,2}'::integer[])]")
      assert.strictEqual(rows.length, 2)
      const ids = rows.map((r: any) => r.id).sort()
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // Existing cast types still work

  describe('existing casts', () => {
    it('interval cast should still work', async () => {
      const row = await oql.queryOne("users { val: ('1 hour'::interval) } [id = 1]")
      assert.ok(row.val !== null)
    })

    it('integer cast should still work', async () => {
      const row = await oql.queryOne("users { val: ('42'::integer) } [id = 1]")
      assert.strictEqual(row.val, 42)
    })
  })
})
