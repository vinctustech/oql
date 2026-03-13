import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, type OQL } from './setup.ts'

describe('Function return types', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL()
  })

  after(() => {
    oql.close?.()
  })

  // String functions

  describe('lower()', () => {
    it('should return lowercase text', async () => {
      const row = await oql.queryOne("users { val: lower(name) } [id = 1]")
      assert.strictEqual(row.val, 'alice')
    })
  })

  describe('upper()', () => {
    it('should return uppercase text', async () => {
      const row = await oql.queryOne("users { val: upper(name) } [id = 1]")
      assert.strictEqual(row.val, 'ALICE')
    })
  })

  describe('length()', () => {
    it('should return string length as integer', async () => {
      const row = await oql.queryOne("users { val: length(name) } [id = 1]")
      assert.strictEqual(row.val, 5)
      assert.strictEqual(typeof row.val, 'number')
    })
  })

  describe('trim()', () => {
    it('should trim whitespace', async () => {
      // name has no whitespace, so trim is identity
      const row = await oql.queryOne("users { val: trim(name) } [id = 1]")
      assert.strictEqual(row.val, 'Alice')
    })
  })

  describe('replace()', () => {
    it('should replace substring', async () => {
      const row = await oql.queryOne("users { val: replace(name, 'Alice', 'Alicia') } [id = 1]")
      assert.strictEqual(row.val, 'Alicia')
    })
  })

  describe('substring()', () => {
    it('should extract substring with start and length', async () => {
      const row = await oql.queryOne("users { val: substring(name, 1, 3) } [id = 1]")
      assert.strictEqual(row.val, 'Ali')
    })
  })

  describe('left() and right()', () => {
    it('left should return first n characters', async () => {
      const row = await oql.queryOne("users { val: left(name, 3) } [id = 1]")
      assert.strictEqual(row.val, 'Ali')
    })

    it('right should return last n characters', async () => {
      const row = await oql.queryOne("users { val: right(name, 3) } [id = 1]")
      assert.strictEqual(row.val, 'ice')
    })
  })

  describe('concat()', () => {
    it('should concatenate two strings', async () => {
      const row = await oql.queryOne("users { val: concat(name, ' Smith') } [id = 1]")
      assert.strictEqual(row.val, 'Alice Smith')
    })

    it('should concatenate three strings', async () => {
      const row = await oql.queryOne("users { val: concat(name, ' <', email) } [id = 1]")
      assert.strictEqual(row.val, 'Alice <alice@example.com')
    })
  })

  // Null handling

  describe('coalesce()', () => {
    it('should return first non-null value', async () => {
      const row = await oql.queryOne("users { val: coalesce(email, 'none') } [id = 1]")
      assert.strictEqual(row.val, 'alice@example.com')
    })

    it('should return fallback when first arg is null', async () => {
      // Use raw SQL to insert a user with null email, test coalesce, then clean up
      await oql.raw("INSERT INTO users (id, name, email, active) VALUES (9999, 'NullEmail', NULL, true)")
      const row = await oql.queryOne("users { val: coalesce(email, 'none') } [id = 9999]")
      assert.strictEqual(row.val, 'none')
      await oql.raw("DELETE FROM users WHERE id = 9999")
    })
  })

  describe('nullif()', () => {
    it('should return null when args are equal', async () => {
      const row = await oql.queryOne("users { val: nullif(name, 'Alice') } [id = 1]")
      assert.strictEqual(row.val, null)
    })

    it('should return first arg when args differ', async () => {
      const row = await oql.queryOne("users { val: nullif(name, 'NotAlice') } [id = 1]")
      assert.strictEqual(row.val, 'Alice')
    })
  })

  // Aggregate functions

  describe('sum()', () => {
    it('should sum integer values', async () => {
      const rows = await oql.queryMany(
        'posts { author_id: &author total: sum(id) } /&author/'
      )
      const sorted = [...rows].sort((a: any, b: any) => a.author_id - b.author_id)
      // Alice (author 1) has posts 1+2=3, Bob (author 2) has post 3
      assert.strictEqual(sorted[0].total, 3)
      assert.strictEqual(sorted[1].total, 3)
    })
  })

  describe('string_agg()', () => {
    it('should aggregate strings with delimiter', async () => {
      const rows = await oql.queryMany(
        "posts { author_id: &author titles: string_agg(title, ', ') } /&author/"
      )
      const alice = rows.find((r: any) => r.author_id === 1)
      // Alice has "First Post" and "Second Post" — order may vary
      assert.ok(alice.titles.includes('First Post'))
      assert.ok(alice.titles.includes('Second Post'))
    })
  })

  // Date/time functions

  describe('now()', () => {
    it('should return a timestamp', async () => {
      const row = await oql.queryOne("users { val: now() } [id = 1]")
      assert.ok(row.val instanceof Date)
    })
  })

  describe('date_trunc()', () => {
    it('should truncate timestamp to day', async () => {
      const row = await oql.queryOne("users { val: date_trunc('year', now()) } [id = 1]")
      assert.ok(row.val instanceof Date)
      // Truncated to year start: Jan 1 of current year
      const d = row.val as Date
      assert.strictEqual(d.getUTCMonth(), 0)
      assert.strictEqual(d.getUTCDate(), 1)
    })
  })

  describe('date_part()', () => {
    it('should extract year as number', async () => {
      const row = await oql.queryOne("users { val: date_part('year', now()) } [id = 1]")
      assert.strictEqual(typeof row.val, 'number')
      assert.strictEqual(row.val, new Date().getFullYear())
    })
  })

  // Math functions

  describe('abs()', () => {
    it('should return absolute value', async () => {
      const row = await oql.queryOne("users { val: abs(id - 10) } [id = 1]")
      assert.strictEqual(row.val, 9)
    })
  })

  describe('ceil()', () => {
    it('should round up with float literal', async () => {
      const row = await oql.queryOne("users { val: ceil(1.1) } [id = 1]")
      assert.strictEqual(row.val, 2)
    })

    it('should round up with mixed-type arithmetic', async () => {
      const row = await oql.queryOne("users { val: ceil(id + 0.1) } [id = 1]")
      assert.strictEqual(row.val, 2)
    })
  })

  describe('floor()', () => {
    it('should round down with float literal', async () => {
      const row = await oql.queryOne("users { val: floor(1.9) } [id = 1]")
      assert.strictEqual(row.val, 1)
    })

    it('should round down with mixed-type arithmetic', async () => {
      const row = await oql.queryOne("users { val: floor(id + 0.9) } [id = 1]")
      assert.strictEqual(row.val, 1)
    })
  })

  describe('round()', () => {
    it('should round to nearest integer', async () => {
      const row = await oql.queryOne("users { val: round(1.5) } [id = 1]")
      assert.strictEqual(row.val, 2)
    })

    it('should round with mixed-type arithmetic', async () => {
      const row = await oql.queryOne("users { val: round(id + 0.5) } [id = 1]")
      assert.strictEqual(row.val, 2)
    })
  })

  // Mixed-type arithmetic promotion

  describe('mixed-type arithmetic', () => {
    it('integer + float should return number', async () => {
      const row = await oql.queryOne("users { val: (id + 0.5) } [id = 1]")
      assert.strictEqual(row.val, 1.5)
      assert.strictEqual(typeof row.val, 'number')
    })

    it('integer * float should return number', async () => {
      const row = await oql.queryOne("users { val: (id * 2.5) } [id = 2]")
      assert.strictEqual(row.val, 5)
      assert.strictEqual(typeof row.val, 'number')
    })

    it('float - integer should return number', async () => {
      const row = await oql.queryOne("users { val: (10.0 - id) } [id = 1]")
      assert.strictEqual(row.val, 9)
      assert.strictEqual(typeof row.val, 'number')
    })
  })

  // Aggregate functions (additional)

  describe('count(*)', () => {
    it('should count all rows per group', async () => {
      const rows = await oql.queryMany(
        'posts { author_id: &author post_count: count(*) } /&author/'
      )
      const sorted = [...rows].sort((a: any, b: any) => a.author_id - b.author_id)
      assert.strictEqual(sorted[0].post_count, 2) // Alice has 2 posts
      assert.strictEqual(sorted[1].post_count, 1) // Bob has 1 post
    })
  })

  describe('min()', () => {
    it('should return minimum value', async () => {
      const rows = await oql.queryMany(
        'posts { author_id: &author first: min(id) } /&author/'
      )
      const alice = rows.find((r: any) => r.author_id === 1)
      assert.strictEqual(alice.first, 1)
    })
  })

  describe('max()', () => {
    it('should return maximum value', async () => {
      const rows = await oql.queryMany(
        'posts { author_id: &author last: max(id) } /&author/'
      )
      const alice = rows.find((r: any) => r.author_id === 1)
      assert.strictEqual(alice.last, 2)
    })
  })

  describe('avg()', () => {
    it('should return average as float', async () => {
      const rows = await oql.queryMany(
        'posts { author_id: &author average: avg(id) } /&author/'
      )
      const alice = rows.find((r: any) => r.author_id === 1)
      assert.strictEqual(typeof alice.average, 'number')
      assert.strictEqual(alice.average, 1.5) // avg(1,2) = 1.5
    })
  })

  describe('bool_and()', () => {
    it('should return AND of all boolean values', async () => {
      // All 3 seed users: Alice(true), Bob(true), Charlie(false)
      const row = await oql.queryOne('users { val: bool_and(active) } [id IN (1,2,3)]')
      assert.strictEqual(row.val, false) // false because Charlie is inactive
    })

    it('should return true when all are true', async () => {
      const row = await oql.queryOne('users { val: bool_and(active) } [id IN (1,2)]')
      assert.strictEqual(row.val, true)
    })
  })

  describe('bool_or()', () => {
    it('should return OR of all boolean values', async () => {
      const row = await oql.queryOne('users { val: bool_or(active) } [id IN (1,2,3)]')
      assert.strictEqual(row.val, true)
    })

    it('should return false when all are false', async () => {
      const row = await oql.queryOne('users { val: bool_or(active) } [id = 3]')
      assert.strictEqual(row.val, false)
    })
  })

  // Functions in WHERE clauses

  describe('functions in WHERE', () => {
    it('lower() in comparison', async () => {
      const rows = await oql.queryMany("users { id } [lower(name) = 'alice']")
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].id, 1)
    })

    it('length() in comparison', async () => {
      const rows = await oql.queryMany("users { id name } [length(name) = 3 AND id IN (1,2,3)]")
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].name, 'Bob')
    })

    it('upper() combined with LIKE', async () => {
      const rows = await oql.queryMany("users { id } [upper(name) LIKE 'AL%']")
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].id, 1)
    })
  })
})
