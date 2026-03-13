import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, mutationSchema, type OQL } from './setup.ts'

describe('OQL queries', () => {
  let oql: OQL
  let mutOql: OQL // Separate instance for tests that insert/modify data

  before(async () => {
    oql = await createOQL()
    mutOql = await createOQL(mutationSchema)
  })

  after(() => {
    oql.close?.()
    mutOql.close?.()
  })

  describe('queryMany', () => {
    it('should return all rows with correct structure', async () => {
      const users = await oql.queryMany<{id: number, name: string}>('users { id name } [id IN (1,2,3)]')
      const sorted = [...users].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, name: 'Alice' },
        { id: 2, name: 'Bob' },
        { id: 3, name: 'Charlie' }
      ])
    })

    it('should return empty array when no matches', async () => {
      const users = await oql.queryMany('users { id } [id = -999]')
      assert.deepStrictEqual(users, [])
    })

    it('should filter with WHERE clause', async () => {
      const users = await oql.queryMany('users { id name } [active = true AND id IN (1,2,3)]')
      const sorted = [...users].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, name: 'Alice' },
        { id: 2, name: 'Bob' }
      ])
    })

    it('should return nested objects for relationships', async () => {
      const posts = await oql.queryMany('posts { id title author { id name } } [id IN (1,2,3)]')
      const sorted = [...posts].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, title: 'First Post', author: { id: 1, name: 'Alice' } },
        { id: 2, title: 'Second Post', author: { id: 1, name: 'Alice' } },
        { id: 3, title: 'Bobs Post', author: { id: 2, name: 'Bob' } }
      ])
    })

    it('should select all fields with *', async () => {
      const users = await oql.queryMany('users { * } [id = 1]')
      assert.deepStrictEqual(users, [{
        id: 1,
        name: 'Alice',
        email: 'alice@example.com',
        active: true
      }])
    })
  })

  describe('queryOne', () => {
    it('should return single object with correct structure', async () => {
      const user = await oql.queryOne<{id: number, name: string, email: string}>('users { id name email } [id = 1]')
      assert.deepStrictEqual(user, {
        id: 1,
        name: 'Alice',
        email: 'alice@example.com'
      })
    })

    it('should return undefined when no match', async () => {
      const user = await oql.queryOne('users { id } [id = -999]')
      assert.strictEqual(user, undefined)
    })

    it('should throw when multiple rows returned', async () => {
      await assert.rejects(
        () => oql.queryOne('users { id } [id IN (1,2)]'),
        /more than one row/
      )
    })
  })

  describe('count', () => {
    it('should return total count', async () => {
      const count = await oql.count('users [id IN (1,2,3)]')
      assert.strictEqual(count, 3)
    })

    it('should return filtered count', async () => {
      const count = await oql.count('users [active = true AND id IN (1,2,3)]')
      assert.strictEqual(count, 2)
    })

    it('should return 0 when no matches', async () => {
      const count = await oql.count('users [id = -999]')
      assert.strictEqual(count, 0)
    })
  })

  describe('comparison operators', () => {
    it('should support greater than', async () => {
      const users = await oql.queryMany('users { id } [id > 1 AND id <= 3]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [2, 3])
    })

    it('should support less than', async () => {
      const users = await oql.queryMany('users { id } [id < 3 AND id >= 1]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should support not equal', async () => {
      const users = await oql.queryMany('users { id } [id != 2 AND id IN (1,2,3)]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 3])
    })
  })

  describe('logical operators', () => {
    it('should support OR', async () => {
      const users = await oql.queryMany('users { id name } [id = 1 OR id = 3]')
      const sorted = [...users].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, name: 'Alice' },
        { id: 3, name: 'Charlie' }
      ])
    })

    it('should support NOT', async () => {
      const users = await oql.queryMany('users { id } [NOT active = true AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 3)
    })

    it('should support complex boolean expressions', async () => {
      const users = await oql.queryMany('users { id } [(id = 1 OR id = 2) AND active = true]')
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })
  })

  describe('string operators', () => {
    it('should support LIKE', async () => {
      const users = await oql.queryMany('users { id name } [name LIKE "A%"]')
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })

    it('should support LIKE with wildcard in middle', async () => {
      const users = await oql.queryMany('users { id name } [name LIKE "%li%"]')
      const names = users.map(u => u.name).sort()
      assert.deepStrictEqual(names, ['Alice', 'Charlie'])
    })

    it('should support case-insensitive ILIKE', async () => {
      const users = await oql.queryMany('users { id name } [name ILIKE "alice"]')
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })
  })

  describe('NULL handling', () => {
    it('should query for NULL values with IS NULL', async () => {
      // Insert a user with null email into mutation table
      const inserted = await mutOql.entity('users').insert({ name: 'NullTest', email: null, active: true })

      // Scope by id since other concurrent tests may also insert into mut_users
      const user = await mutOql.queryOne('users { id name email } [id = :id AND email IS NULL]', { id: inserted.id })
      assert.deepStrictEqual(user, { id: inserted.id, name: 'NullTest', email: null })

      // Verify IS NULL does NOT match rows with non-null emails
      const noMatch = await mutOql.queryOne('users { id } [id = 1 AND email IS NULL]')
      assert.strictEqual(noMatch, undefined)

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should query for non-NULL values with IS NOT NULL', async () => {
      const users = await oql.queryMany('users { id name } [email IS NOT NULL AND id IN (1,2,3)]')
      const sorted = [...users].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, name: 'Alice' },
        { id: 2, name: 'Bob' },
        { id: 3, name: 'Charlie' }
      ])
    })
  })

  describe('ordering', () => {
    it('should order ascending', async () => {
      const users = await oql.queryMany('users { id name } [id IN (1,2,3)] <name>')
      assert.deepStrictEqual(users.map(u => u.name), ['Alice', 'Bob', 'Charlie'])
    })

    it('should order descending', async () => {
      const users = await oql.queryMany('users { id name } [id IN (1,2,3)] <name DESC>')
      assert.deepStrictEqual(users.map(u => u.name), ['Charlie', 'Bob', 'Alice'])
    })

    it('should order by multiple fields', async () => {
      const posts = await oql.queryMany('posts { id title author { id } } [id IN (1,2,3)] <author.id DESC, title ASC>')
      // Author 2 (Bob) first, then Author 1 (Alice) sorted by title ASC
      assert.strictEqual(posts.length, 3)
      assert.deepStrictEqual(posts.map(p => p.title), ['Bobs Post', 'First Post', 'Second Post'])
    })

    it('should not conflict with > in ordering closure when followed by more projections', async () => {
      const users = await oql.queryMany('users { id posts { id title } <title> name } [id IN (1,2)] <id>')
      assert.deepStrictEqual(users, [
        { id: 1, posts: [{ id: 1, title: 'First Post' }, { id: 2, title: 'Second Post' }], name: 'Alice' },
        { id: 2, posts: [{ id: 3, title: 'Bobs Post' }], name: 'Bob' },
      ])
    })

    it('should handle subquery ordering followed by sibling projections', async () => {
      const users = await oql.queryMany('users { id posts { title } <title DESC> active } [id = 1]')
      assert.deepStrictEqual(users, [
        { id: 1, posts: [{ title: 'Second Post' }, { title: 'First Post' }], active: true },
      ])
    })
  })

  describe('limit and offset', () => {
    it('should limit results', async () => {
      const users = await oql.queryMany('users { id } [id IN (1,2,3)] |2|')
      assert.strictEqual(users.length, 2)
    })

    it('should offset results', async () => {
      const users = await oql.queryMany('users { id } [id IN (1,2,3)] <id> |2, 1|')
      assert.deepStrictEqual(users.map(u => u.id), [2, 3])
    })
  })

  describe('parameters', () => {
    it('should substitute string parameters', async () => {
      const users = await oql.queryMany('users { id name } [name = :name]', { name: 'Alice' })
      assert.deepStrictEqual(users, [{ id: 1, name: 'Alice' }])
    })

    it('should substitute number parameters', async () => {
      const user = await oql.queryOne('users { id name } [id = :id]', { id: 1 })
      assert.deepStrictEqual(user, { id: 1, name: 'Alice' })
    })

    it('should substitute boolean parameters', async () => {
      const users = await oql.queryMany('users { id } [active = :active AND id = 3]', { active: false })
      assert.deepStrictEqual(users, [{ id: 3 }])
    })

    it('should substitute array parameters with IN', async () => {
      const users = await oql.queryMany('users { id name } [id IN :ids]', { ids: [1, 3] })
      const sorted = [...users].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, name: 'Alice' },
        { id: 3, name: 'Charlie' }
      ])
    })

    it('should handle strings with special characters', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: "O'Brien",
        email: 'obrien@example.com',
        active: true
      })

      const users = await mutOql.queryMany('users { id name } [name = :name]', { name: "O'Brien" })
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, "O'Brien")

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should handle strings with backslashes', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: 'path\\to\\file',
        email: 'path@example.com',
        active: true
      })

      const users = await mutOql.queryMany('users { id name } [name = :name]', { name: 'path\\to\\file' })
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'path\\to\\file')

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })
  })

  describe('relationship queries', () => {
    it('should filter by relationship field', async () => {
      const posts = await oql.queryMany('posts { id title } [author.name = "Alice"]')
      const sorted = [...posts].sort((a, b) => a.id - b.id)
      assert.deepStrictEqual(sorted, [
        { id: 1, title: 'First Post' },
        { id: 2, title: 'Second Post' }
      ])
    })

    it('should return null for missing relationships', async () => {
      // Insert a post with no author into mutation table
      await mutOql.raw('INSERT INTO mut_posts (id, title, body, author) VALUES (999, \'Orphan Post\', \'No author\', NULL)')

      const post = await mutOql.queryOne('posts { id title author { name } } [id = 999]')
      assert.deepStrictEqual(post, {
        id: 999,
        title: 'Orphan Post',
        author: null
      })

      // Cleanup
      await mutOql.raw('DELETE FROM mut_posts WHERE id = 999')
    })
  })

  describe('edge cases', () => {
    it('should handle unicode characters', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: '日本語テスト',
        email: 'unicode@example.com',
        active: true
      })

      const user = await mutOql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: '日本語テスト' })

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should handle emoji characters', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: '👨‍💻 Developer',
        email: 'emoji@example.com',
        active: true
      })

      const user = await mutOql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: '👨‍💻 Developer' })

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should handle very long strings', async () => {
      const longName = 'A'.repeat(200)
      const inserted = await mutOql.entity('users').insert({
        name: longName,
        email: 'long@example.com',
        active: true
      })

      const user = await mutOql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: longName })

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should handle newlines in strings', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: 'Line1\nLine2',
        email: 'newline@example.com',
        active: true
      })

      const user = await mutOql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: 'Line1\nLine2' })

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should handle tabs in strings', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: 'Col1\tCol2',
        email: 'tab@example.com',
        active: true
      })

      const user = await mutOql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: 'Col1\tCol2' })

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })

    it('should handle empty IN array with manual SQL', async () => {
      const users = await oql.raw('SELECT id FROM users WHERE id = ANY($1)', [[]])
      assert.deepStrictEqual(users, [])
    })

    it('should handle multiple parameters in complex query', async () => {
      const users = await oql.queryMany(
        'users { id name } [name LIKE :pattern AND active = :active AND id > :minId AND id IN (1,2,3)]',
        { pattern: '%li%', active: true, minId: 0 }
      )
      // Alice contains 'li' and is active; Charlie contains 'li' but is inactive
      assert.deepStrictEqual(users, [{ id: 1, name: 'Alice' }])
    })

    it('should handle single quotes in LIKE parameter', async () => {
      const inserted = await mutOql.entity('users').insert({
        name: "It's a test",
        email: 'quote@example.com',
        active: true
      })

      const users = await mutOql.queryMany(
        "users { name } [name LIKE :pattern]",
        { pattern: "%It's%" }
      )
      assert.deepStrictEqual(users, [{ name: "It's a test" }])

      // Cleanup
      await mutOql.entity('users').delete(inserted.id)
    })
  })

  describe('aggregate functions', () => {
    it('should count with COUNT function', async () => {
      const result = await oql.raw<{count: string}>(
        'SELECT COUNT(*) as count FROM users WHERE id IN (1,2,3)'
      )
      assert.strictEqual(result.length, 1)
      assert.strictEqual(parseInt(result[0].count), 3)
    })
  })

  describe('star-minus projection', () => {
    it('should exclude a single field with * -field', async () => {
      const users = await oql.queryMany('users { * -email } [id = 1]')
      assert.deepStrictEqual(users, [{
        id: 1,
        name: 'Alice',
        active: true
      }])
    })

    it('should exclude multiple fields with * -field1 -field2', async () => {
      const users = await oql.queryMany('users { * -email -active } [id = 1]')
      assert.deepStrictEqual(users, [{
        id: 1,
        name: 'Alice'
      }])
    })
  })

  describe('NULLS FIRST / NULLS LAST', () => {
    it('should order with NULLS FIRST', async () => {
      // Insert a user with null email
      const inserted = await mutOql.entity('users').insert({ name: 'NullSort', email: null, active: true })

      const users = await mutOql.queryMany(
        'users { id email } [id IN :ids] <email ASC NULLS FIRST>',
        { ids: [1, inserted.id] }
      )
      // null should come first
      assert.strictEqual(users[0].email, null)
      assert.strictEqual(users[1].email, 'alice@example.com')

      await mutOql.entity('users').delete(inserted.id)
    })

    it('should order with NULLS LAST', async () => {
      const inserted = await mutOql.entity('users').insert({ name: 'NullSort', email: null, active: true })

      const users = await mutOql.queryMany(
        'users { id email } [id IN :ids] <email ASC NULLS LAST>',
        { ids: [1, inserted.id] }
      )
      // null should come last
      assert.strictEqual(users[0].email, 'alice@example.com')
      assert.strictEqual(users[1].email, null)

      await mutOql.entity('users').delete(inserted.id)
    })
  })

  describe('built-in variables', () => {
    it('current_date should return a date', async () => {
      const row = await oql.queryOne("users { val: current_date } [id = 1]")
      assert.ok(row.val instanceof Date || typeof row.val === 'string')
    })

    it('current_timestamp should return a timestamp', async () => {
      const row = await oql.queryOne("users { val: current_timestamp } [id = 1]")
      assert.ok(row.val instanceof Date)
    })
  })

  describe('unary minus', () => {
    it('should negate a field value', async () => {
      const row = await oql.queryOne("users { val: (-id) } [id = 1]")
      assert.strictEqual(row.val, -1)
    })

    it('should negate an expression', async () => {
      const row = await oql.queryOne("users { val: (-(id + 5)) } [id = 1]")
      assert.strictEqual(row.val, -6)
    })

    it('should work in WHERE clause', async () => {
      const rows = await oql.queryMany("users { id } [-id < -2 AND id IN (1,2,3)]")
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].id, 3)
    })
  })

  describe('offset-only restrict', () => {
    it('should support offset without limit using |,offset|', async () => {
      const users = await oql.queryMany('users { id } [id IN (1,2,3)] <id> |,1|')
      assert.deepStrictEqual(users.map((u: any) => u.id), [2, 3])
    })
  })
})
