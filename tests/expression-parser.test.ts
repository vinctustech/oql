import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, type OQL } from './setup.ts'

describe('unified expression parser', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL()
  })

  after(() => {
    oql.close?.()
  })

  // --- Group 1: Function calls as standalone boolean (was broken) ---

  describe('function calls in boolean context', () => {
    it('should accept function call as standalone boolean', async () => {
      // coalesce(active, true) returns the first non-null arg — active is non-null for all 3 users
      // Alice(true), Bob(true), Charlie(false) → only truthy ones returned
      const users = await oql.queryMany('users { id name } [coalesce(active, true) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should accept NOT function_call()', async () => {
      // Charlie has active=false, coalesce(false, false)=false, NOT false=true
      const users = await oql.queryMany('users { id name } [NOT coalesce(active, false) AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'Charlie')
    })

    it('should accept function call in OR branch', async () => {
      // id = 999 matches nobody; coalesce(active, true) AND id IN (1,2,3) → Alice and Bob
      const users = await oql.queryMany('users { id } [id = 999 OR coalesce(active, true) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should accept function call in AND branch', async () => {
      // id > 0 AND coalesce(active, true) AND id IN (1,2,3) → Alice and Bob
      const users = await oql.queryMany('users { id } [id > 0 AND coalesce(active, true) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should accept two function calls combined with OR', async () => {
      // coalesce(active, true) OR coalesce(active, false) → all active users match first branch
      // Due to AND precedence: coalesce(active,true) OR (coalesce(active,false) AND id IN (1,2,3))
      // First branch has no id constraint so matches all active users; second adds Charlie(false)=false
      const users = await oql.queryMany('users { id } [(coalesce(active, true) OR coalesce(active, false)) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 2: CASE expression as standalone boolean (was broken) ---

  describe('CASE expression in boolean context', () => {
    it('should accept CASE returning boolean as condition', async () => {
      const users = await oql.queryMany(
        'users { id name } [CASE WHEN id = 1 THEN true ELSE false END]'
      )
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })

    it('should accept multi-branch CASE as boolean', async () => {
      const users = await oql.queryMany(
        'users { id name } [CASE WHEN id = 1 THEN true WHEN id = 2 THEN active ELSE false END]'
      )
      assert.strictEqual(users.length, 2)
      const names = users.map(u => u.name).sort()
      assert.deepStrictEqual(names, ['Alice', 'Bob'])
    })

    it('should accept CASE combined with AND', async () => {
      const users = await oql.queryMany(
        'users { id } [id > 0 AND CASE WHEN active = true THEN true ELSE false END AND id IN (1,2,3)]'
      )
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 3: Cast expression as standalone boolean (was broken) ---

  describe('cast expression in boolean context', () => {
    it('should accept cast to boolean as condition', async () => {
      const users = await oql.queryMany('users { id } [active::boolean AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 4: Parenthesized expression in boolean context (was broken) ---

  describe('parenthesized expressions in boolean context', () => {
    it('should accept parenthesized function call as boolean', async () => {
      const users = await oql.queryMany('users { id } [(coalesce(active, true)) AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 5: Arithmetic in comparisons ---

  describe('arithmetic in comparisons', () => {
    it('should parse addition on left side of comparison', async () => {
      const users = await oql.queryMany('users { id } [id + 1 = 2]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
    })

    it('should parse subtraction on right side of comparison', async () => {
      const users = await oql.queryMany('users { id } [id = 3 - 2]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
    })

    it('should parse multiplication in comparison', async () => {
      const users = await oql.queryMany('users { id } [id * 1 = 1]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
    })

    it('should combine arithmetic and logical AND', async () => {
      const users = await oql.queryMany('users { id } [id + 0 > 0 AND id < 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 6: BETWEEN + AND precedence (critical) ---

  describe('BETWEEN and AND precedence', () => {
    it('should parse basic BETWEEN', async () => {
      const users = await oql.queryMany('users { id } [id BETWEEN 1 AND 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })

    it('should not confuse BETWEEN AND with logical AND', async () => {
      // id BETWEEN 1 AND 3 must be parsed as (id BETWEEN 1 AND 3), not (id BETWEEN 1) AND (3)
      const users = await oql.queryMany('users { id } [id BETWEEN 1 AND 3 AND active = true]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should handle logical AND before BETWEEN', async () => {
      const users = await oql.queryMany('users { id } [active = true AND id BETWEEN 1 AND 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should handle BETWEEN combined with OR', async () => {
      const users = await oql.queryMany('users { id } [id BETWEEN 1 AND 2 OR id = 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })

    it('should parse NOT BETWEEN', async () => {
      const users = await oql.queryMany('users { id } [id NOT BETWEEN 1 AND 2 AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 3)
    })
  })

  // --- Group 7: IS NULL / IS NOT NULL ---

  describe('IS NULL / IS NOT NULL', () => {
    it('should handle IS NOT NULL combined with AND', async () => {
      const users = await oql.queryMany('users { id } [email IS NOT NULL AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })

    it('should handle IS NOT NULL combined with BETWEEN', async () => {
      const users = await oql.queryMany('users { id } [email IS NOT NULL AND id BETWEEN 1 AND 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })
  })

  // --- Group 8: IN expressions ---

  describe('IN expressions', () => {
    it('should parse IN array', async () => {
      const users = await oql.queryMany('users { id } [id IN (1, 2)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should parse NOT IN', async () => {
      const users = await oql.queryMany('users { id } [id NOT IN (1, 2) AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 3)
    })
  })

  // --- Group 9: Comparison operators (regression) ---

  describe('comparison operators', () => {
    it('should support equals', async () => {
      const users = await oql.queryMany('users { id } [id = 1]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
    })

    it('should support not equals', async () => {
      const users = await oql.queryMany('users { id } [id != 1 AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [2, 3])
    })

    it('should support greater/less than', async () => {
      const users = await oql.queryMany('users { id } [id > 1 AND id < 3]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 2)
    })

    it('should support greater/less than or equal', async () => {
      const users = await oql.queryMany('users { id } [id >= 1 AND id <= 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })

    it('should support LIKE', async () => {
      const users = await oql.queryMany('users { id name } [name LIKE "A%"]')
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })

    it('should support ILIKE', async () => {
      const users = await oql.queryMany('users { id name } [name ILIKE "alice"]')
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })

    it('should support NOT LIKE', async () => {
      const users = await oql.queryMany('users { id } [name NOT LIKE "A%" AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [2, 3])
    })

    it('should support NOT ILIKE', async () => {
      const users = await oql.queryMany('users { id } [name NOT ILIKE "alice" AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [2, 3])
    })
  })

  // --- Group 10: Logical operators (regression) ---

  describe('logical operators', () => {
    it('should support OR', async () => {
      const users = await oql.queryMany('users { id } [id = 1 OR id = 2]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should support AND', async () => {
      const users = await oql.queryMany('users { id } [id = 1 AND active = true]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
    })

    it('should support NOT', async () => {
      const users = await oql.queryMany('users { id } [NOT id = 1 AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [2, 3])
    })

    it('should support grouped boolean', async () => {
      const users = await oql.queryMany('users { id } [(id = 1 OR id = 2) AND active = true]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should support NOT applied to grouped expression', async () => {
      const users = await oql.queryMany('users { id } [NOT (id = 1 OR id = 2) AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 3)
    })

    it('should support double NOT', async () => {
      const users = await oql.queryMany('users { id } [NOT NOT active AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 11: Boolean literals (regression) ---

  describe('boolean literals', () => {
    it('should accept true as standalone condition', async () => {
      const users = await oql.queryMany('users { id } [true AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })

    it('should accept false as standalone condition', async () => {
      const users = await oql.queryMany('users { id } [false AND id IN (1,2,3)]')
      assert.deepStrictEqual(users, [])
    })

    it('should accept boolean literal in comparison', async () => {
      const users = await oql.queryMany('users { id } [active = true AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  // --- Group 12: Complex nested expressions ---

  describe('complex expressions', () => {
    it('should parse mixed arithmetic, comparison, and logic', async () => {
      const users = await oql.queryMany('users { id } [id + 1 > 1 AND name LIKE "A%" OR id = 3]')
      // (id+1 > 1 AND name LIKE 'A%') OR (id = 3) => Alice and Charlie
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 3])
    })

    it('should parse CASE in projection with boolean WHEN', async () => {
      const users = await oql.queryMany(
        'users { id status: (CASE WHEN active = true THEN \'active\' ELSE \'inactive\' END) } [id = 1]'
      )
      assert.deepStrictEqual(users, [{ id: 1, status: 'active' }])
    })
  })

  // --- Group 13: Missing operators and syntax ---

  describe('string concatenation (||)', () => {
    it('should concatenate strings with ||', async () => {
      const users = await oql.queryMany('users { full: (name || \' test\') } [id = 1]')
      assert.deepStrictEqual(users, [{ full: 'Alice test' }])
    })

    it('should chain multiple || operators', async () => {
      const users = await oql.queryMany('users { full: (name || \' <\' || email || \'>\') } [id = 1]')
      assert.deepStrictEqual(users, [{ full: 'Alice <alice@example.com>' }])
    })
  })

  describe('modulo operator (%)', () => {
    it('should filter with modulo', async () => {
      const users = await oql.queryMany('users { id } [id % 2 = 1 AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 3])
    })

    it('should use modulo in projection', async () => {
      const users = await oql.queryMany('users { id rem: (id % 2) } [id = 3]')
      assert.deepStrictEqual(users, [{ id: 3, rem: 1 }])
    })
  })

  describe('zero-argument function calls', () => {
    it('should accept now() with no arguments', async () => {
      const users = await oql.queryMany('users { id ts: now() } [id = 1]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
      assert.ok(users[0].ts instanceof Date || !isNaN(new Date(users[0].ts).getTime()))
    })

    it('should accept random() with no arguments', async () => {
      const users = await oql.queryMany('users { id r: random() } [id = 1]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 1)
      assert.strictEqual(typeof users[0].r, 'number')
      assert.ok(users[0].r >= 0 && users[0].r < 1)
    })

    it('should accept zero-arg function in WHERE', async () => {
      const users = await oql.queryMany('users { id } [random() > -1 AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2, 3])
    })
  })

  // --- Group 14: DISTINCT ---

  describe('DISTINCT', () => {
    it('should deduplicate rows with DISTINCT', async () => {
      // All 3 users have unique names, so DISTINCT returns 3 rows
      const users = await oql.queryMany('^ users { name } [id IN (1,2,3)] <name>')
      assert.strictEqual(users.length, 3)
      const names = users.map(u => u.name)
      assert.deepStrictEqual(names, ['Alice', 'Bob', 'Charlie'])
    })

    it('should deduplicate identical values with DISTINCT', async () => {
      // Alice(true), Bob(true), Charlie(false) → DISTINCT active gives 2 rows
      const rows = await oql.queryMany('^ users { active } [id IN (1,2,3)] <active DESC>')
      assert.strictEqual(rows.length, 2)
      const values = rows.map(r => r.active)
      assert.deepStrictEqual(values, [true, false])
    })
  })

  // --- Group 15: DISTINCT ON ---

  describe('DISTINCT ON', () => {
    it('should return one row per distinct key', async () => {
      // posts: (1, 'First Post', author=1/Alice), (2, 'Second Post', author=1/Alice), (3, 'Bobs Post', author=2/Bob)
      // DISTINCT ON (&author) with ORDER BY &author, id → one post per author, earliest id wins
      const posts = await oql.queryMany('^(&author) posts { &author title } [id IN (1,2,3)] <&author, id>')
      assert.strictEqual(posts.length, 2)
      const titles = posts.map(p => p.title).sort()
      assert.deepStrictEqual(titles, ['Bobs Post', 'First Post'])
    })

    it('should handle dotted reference in DISTINCT ON (induces join)', async () => {
      // DISTINCT ON (author.name) — traverses FK to users table, inducing a LEFT JOIN
      // Alice authored posts 1 and 2, Bob authored post 3
      // DISTINCT ON author.name with ORDER BY author.name, id → one post per author name
      const posts = await oql.queryMany('^(author.name) posts { name: author.name title } [id IN (1,2,3)] <author.name, id>')
      assert.strictEqual(posts.length, 2)
      const rows = posts.map(p => ({ name: p.name, title: p.title })).sort((a, b) => a.name.localeCompare(b.name))
      assert.deepStrictEqual(rows, [
        { name: 'Alice', title: 'First Post' },
        { name: 'Bob', title: 'Bobs Post' },
      ])
    })
  })

  // --- Group 16: HAVING ---

  describe('HAVING', () => {
    it('should filter groups with HAVING', async () => {
      // Alice has 2 posts, Bob has 1 → only Alice with count > 1
      const rows = await oql.queryMany('posts { &author post_count: count(id) } /&author [count(id) > 1]/')
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].author, 1)
      assert.strictEqual(rows[0].post_count, 2)
    })

    it('should filter groups with HAVING using >=', async () => {
      // active=true: Alice(1), Bob(2) → 2 users; active=false: Charlie(3) → 1 user
      // HAVING count(id) >= 2 → only active=true group
      const rows = await oql.queryMany('users { active user_count: count(id) } /active [count(id) >= 2]/')
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].active, true)
      assert.strictEqual(rows[0].user_count, 2)
    })

    it('should handle dotted reference in GROUP BY with HAVING (induces join)', async () => {
      // GROUP BY author.name — traverses FK to users table, inducing a LEFT JOIN
      // Alice has 2 posts, Bob has 1 → HAVING count(id) > 1 → only Alice
      const rows = await oql.queryMany('posts { name: author.name post_count: count(id) } /author.name [count(id) > 1]/')
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].name, 'Alice')
      assert.strictEqual(rows[0].post_count, 2)
    })
  })

  // --- Group 17: Expressions in ordering context (regression) ---

  describe('expressions in ORDER BY', () => {
    it('should accept arithmetic in ORDER BY', async () => {
      const users = await oql.queryMany('users { id name } [id IN (1,2,3)] <id + 0>')
      assert.strictEqual(users.length, 3)
      assert.deepStrictEqual(users.map(u => u.id), [1, 2, 3])
    })

    it('should accept CASE in ORDER BY', async () => {
      const users = await oql.queryMany(
        'users { id name } [id IN (1,2,3)] <CASE WHEN id = 1 THEN 0 ELSE 1 END, id>'
      )
      // Alice (id=1) first (CASE=0), then Bob and Charlie ordered by id (CASE=1)
      assert.strictEqual(users.length, 3)
      assert.deepStrictEqual(users.map(u => u.id), [1, 2, 3])
    })
  })
})
