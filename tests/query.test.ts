import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { createOQL } from './setup.ts'

describe('OQL queries', () => {
  let oql: OQL

  before(() => {
    oql = createOQL()
  })

  after(() => {
    oql.close()
  })

  describe('queryMany', () => {
    it('should return all rows with correct structure', async () => {
      const users = await oql.queryMany<{id: number, name: string}>('users { id name } [id IN (1,2,3)]')
      assert.strictEqual(users.length, 3)

      const alice = users.find(u => u.id === 1)
      assert.deepStrictEqual(alice, { id: 1, name: 'Alice' })
    })

    it('should return empty array when no matches', async () => {
      const users = await oql.queryMany('users { id } [id = -999]')
      assert.deepStrictEqual(users, [])
    })

    it('should filter with WHERE clause', async () => {
      const users = await oql.queryMany('users { id name } [active = true AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 2)

      const names = users.map(u => u.name).sort()
      assert.deepStrictEqual(names, ['Alice', 'Bob'])
    })

    it('should return nested objects for relationships', async () => {
      const posts = await oql.queryMany('posts { id title author { id name } } [id IN (1,2,3)]')
      assert.strictEqual(posts.length, 3)

      const firstPost = posts.find(p => p.id === 1)
      assert.deepStrictEqual(firstPost, {
        id: 1,
        title: 'First Post',
        author: { id: 1, name: 'Alice' }
      })
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
      assert.strictEqual(users.length, 2)
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [2, 3])
    })

    it('should support less than', async () => {
      const users = await oql.queryMany('users { id } [id < 3 AND id >= 1]')
      assert.strictEqual(users.length, 2)
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 2])
    })

    it('should support not equal', async () => {
      const users = await oql.queryMany('users { id } [id != 2 AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 2)
      const ids = users.map(u => u.id).sort((a, b) => a - b)
      assert.deepStrictEqual(ids, [1, 3])
    })
  })

  describe('logical operators', () => {
    it('should support OR', async () => {
      const users = await oql.queryMany('users { id name } [id = 1 OR id = 3]')
      assert.strictEqual(users.length, 2)
      const names = users.map(u => u.name).sort()
      assert.deepStrictEqual(names, ['Alice', 'Charlie'])
    })

    it('should support NOT', async () => {
      const users = await oql.queryMany('users { id } [NOT active = true AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].id, 3)
    })

    it('should support complex boolean expressions', async () => {
      const users = await oql.queryMany('users { id } [(id = 1 OR id = 2) AND active = true]')
      assert.strictEqual(users.length, 2)
    })
  })

  describe('string operators', () => {
    it('should support LIKE', async () => {
      const users = await oql.queryMany('users { id name } [name LIKE "A%"]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'Alice')
    })

    it('should support LIKE with wildcard in middle', async () => {
      const users = await oql.queryMany('users { id name } [name LIKE "%li%"]')
      const names = users.map(u => u.name).sort()
      assert.deepStrictEqual(names, ['Alice', 'Charlie'])
    })

    it('should support case-insensitive ILIKE', async () => {
      const users = await oql.queryMany('users { id name } [name ILIKE "alice"]')
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'Alice')
    })
  })

  describe('NULL handling', () => {
    it('should query for NULL values with IS NULL', async () => {
      // Insert a user with null email
      const inserted = await oql.entity('users').insert({ name: 'NullTest', email: null, active: true })

      const users = await oql.queryMany('users { id name } [email IS NULL]')
      assert.ok(users.length >= 1)
      assert.ok(users.some(u => u.name === 'NullTest'))

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should query for non-NULL values with IS NOT NULL', async () => {
      const users = await oql.queryMany('users { id name } [email IS NOT NULL AND id IN (1,2,3)]')
      assert.strictEqual(users.length, 3) // All seed users have emails
    })
  })

  describe('ordering', () => {
    it('should order ascending', async () => {
      const users = await oql.queryMany('users { id name } [id IN (1,2,3)] <name>')
      assert.strictEqual(users[0].name, 'Alice')
      assert.strictEqual(users[2].name, 'Charlie')
    })

    it('should order descending', async () => {
      const users = await oql.queryMany('users { id name } [id IN (1,2,3)] <name DESC>')
      assert.strictEqual(users[0].name, 'Charlie')
      assert.strictEqual(users[2].name, 'Alice')
    })

    it('should order by multiple fields', async () => {
      const posts = await oql.queryMany('posts { id title author { id } } [id IN (1,2,3)] <author.id DESC, title ASC>')
      // Author 2 (Bob) has 1 post, Author 1 (Alice) has 2 posts
      assert.strictEqual(posts[0].author.id, 2) // Bob's post first (DESC by author.id)
    })
  })

  describe('limit and offset', () => {
    it('should limit results', async () => {
      const users = await oql.queryMany('users { id } [id IN (1,2,3)] |2|')
      assert.strictEqual(users.length, 2)
    })

    it('should offset results', async () => {
      const users = await oql.queryMany('users { id } [id IN (1,2,3)] <id> |2, 1|')
      assert.strictEqual(users.length, 2)
      assert.strictEqual(users[0].id, 2) // Skipped id=1
    })
  })

  describe('parameters', () => {
    it('should substitute string parameters', async () => {
      const users = await oql.queryMany('users { id name } [name = :name]', { name: 'Alice' })
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 1, name: 'Alice' })
    })

    it('should substitute number parameters', async () => {
      const user = await oql.queryOne('users { id name } [id = :id]', { id: 1 })
      assert.deepStrictEqual(user, { id: 1, name: 'Alice' })
    })

    it('should substitute boolean parameters', async () => {
      const users = await oql.queryMany('users { id } [active = :active AND id = 3]', { active: false })
      assert.strictEqual(users.length, 1)
      assert.deepStrictEqual(users[0], { id: 3 })
    })

    it('should substitute array parameters with IN', async () => {
      const users = await oql.queryMany('users { id name } [id IN :ids]', { ids: [1, 3] })
      assert.strictEqual(users.length, 2)
      const names = users.map(u => u.name).sort()
      assert.deepStrictEqual(names, ['Alice', 'Charlie'])
    })

    it('should handle strings with special characters', async () => {
      const inserted = await oql.entity('users').insert({
        name: "O'Brien",
        email: 'obrien@example.com',
        active: true
      })

      const users = await oql.queryMany('users { id name } [name = :name]', { name: "O'Brien" })
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, "O'Brien")

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should handle strings with backslashes', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'path\\to\\file',
        email: 'path@example.com',
        active: true
      })

      const users = await oql.queryMany('users { id name } [name = :name]', { name: 'path\\to\\file' })
      assert.strictEqual(users.length, 1)
      assert.strictEqual(users[0].name, 'path\\to\\file')

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })
  })

  describe('relationship queries', () => {
    it('should filter by relationship field', async () => {
      const posts = await oql.queryMany('posts { id title } [author.name = "Alice"]')
      assert.strictEqual(posts.length, 2) // Alice has 2 posts
    })

    it('should return null for missing relationships', async () => {
      // Insert a post with no author
      await oql.raw('INSERT INTO posts (id, title, body, author) VALUES (999, \'Orphan Post\', \'No author\', NULL)')

      const post = await oql.queryOne('posts { id title author { name } } [id = 999]')
      assert.deepStrictEqual(post, {
        id: 999,
        title: 'Orphan Post',
        author: null
      })

      // Cleanup
      await oql.raw('DELETE FROM posts WHERE id = 999')
    })
  })

  describe('edge cases', () => {
    it('should handle unicode characters', async () => {
      const inserted = await oql.entity('users').insert({
        name: '日本語テスト',
        email: 'unicode@example.com',
        active: true
      })

      const user = await oql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: '日本語テスト' })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should handle emoji characters', async () => {
      const inserted = await oql.entity('users').insert({
        name: '👨‍💻 Developer',
        email: 'emoji@example.com',
        active: true
      })

      const user = await oql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: '👨‍💻 Developer' })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should handle very long strings', async () => {
      const longName = 'A'.repeat(200)
      const inserted = await oql.entity('users').insert({
        name: longName,
        email: 'long@example.com',
        active: true
      })

      const user = await oql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: longName })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should handle newlines in strings', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'Line1\nLine2',
        email: 'newline@example.com',
        active: true
      })

      const user = await oql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: 'Line1\nLine2' })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should handle tabs in strings', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'Col1\tCol2',
        email: 'tab@example.com',
        active: true
      })

      const user = await oql.queryOne('users { name } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(user, { name: 'Col1\tCol2' })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should handle empty IN array with manual SQL', async () => {
      // Empty IN clause should return no results
      // This tests the edge case - OQL might not support this directly
      const users = await oql.raw('SELECT id FROM users WHERE id = ANY($1)', [[]])
      assert.deepStrictEqual(users, [])
    })

    it('should handle multiple parameters in complex query', async () => {
      const users = await oql.queryMany(
        'users { id name } [name LIKE :pattern AND active = :active AND id > :minId]',
        { pattern: '%li%', active: true, minId: 0 }
      )

      // Should find Alice (contains 'li', active, id > 0)
      const names = users.map(u => u.name).sort()
      assert.ok(names.includes('Alice'))
    })

    it('should handle single quotes in LIKE parameter', async () => {
      const inserted = await oql.entity('users').insert({
        name: "It's a test",
        email: 'quote@example.com',
        active: true
      })

      const users = await oql.queryMany(
        "users { name } [name LIKE :pattern]",
        { pattern: "%It's%" }
      )
      assert.ok(users.some(u => u.name === "It's a test"))

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })
  })

  describe('aggregate functions', () => {
    it('should count with COUNT function', async () => {
      const result = await oql.raw<{count: string}>(
        'SELECT COUNT(*) as count FROM users WHERE id IN (1,2,3)'
      )
      assert.strictEqual(parseInt(result[0].count), 3)
    })
  })
})
