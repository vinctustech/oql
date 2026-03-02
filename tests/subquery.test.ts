import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { createOQL } from './setup.ts'

describe('Subqueries', () => {
  let oql: OQL

  before(() => {
    oql = createOQL()
  })

  after(() => {
    oql.close()
  })

  describe('EXISTS', () => {
    it('should return rows where subquery has results', async () => {
      // Users who have at least one post (auto-correlated through posts relationship)
      const rows = await oql.queryMany(
        'users { id name } [EXISTS(posts) AND id IN (1,2,3)] <id>'
      )
      assert.strictEqual(rows.length, 2)
      assert.deepStrictEqual(rows.map((r: any) => r.name), ['Alice', 'Bob'])
    })

    it('should filter within EXISTS subquery', async () => {
      // Users who have a post with title starting with 'First'
      const rows = await oql.queryMany(
        'users { id name } [EXISTS(posts [title LIKE "First%"]) AND id IN (1,2,3)] <id>'
      )
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].name, 'Alice')
    })

    it('should work with NOT EXISTS', async () => {
      // Users who have no posts
      const rows = await oql.queryMany(
        'users { id name } [NOT EXISTS(posts) AND id IN (1,2,3)]'
      )
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].name, 'Charlie')
    })
  })

  describe('IN with subquery', () => {
    it('should filter with IN subquery', async () => {
      // Users whose id appears as a post author
      const rows = await oql.queryMany(
        'users { id name } [id IN (posts { &author }) AND id IN (1,2,3)] <id>'
      )
      assert.strictEqual(rows.length, 2)
      assert.deepStrictEqual(rows.map((r: any) => r.name), ['Alice', 'Bob'])
    })

    it('should filter with NOT IN subquery', async () => {
      // Users whose id does NOT appear as a post author
      const rows = await oql.queryMany(
        'users { id name } [id NOT IN (posts { &author }) AND id IN (1,2,3)]'
      )
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].name, 'Charlie')
    })
  })

  describe('Nested query with aggregate', () => {
    it('should return aggregate in nested query result', async () => {
      // One-to-many nested query with count returns array of objects
      const rows = await oql.queryMany(
        'users { id name posts { cnt: count(*) } } [id IN (1,2,3)] <id>'
      )
      assert.strictEqual(rows.length, 3)
      assert.deepStrictEqual(rows[0].posts, [{ cnt: 2 }]) // Alice has 2 posts
      assert.deepStrictEqual(rows[1].posts, [{ cnt: 1 }]) // Bob has 1 post
      assert.deepStrictEqual(rows[2].posts, [{ cnt: 0 }]) // Charlie has 0 posts
    })
  })

  describe('One-to-many nested query', () => {
    it('should return nested array of related entities', async () => {
      // Users with their posts as nested objects
      const rows = await oql.queryMany(
        'users { id name posts { id title } } [id IN (1,2)] <id>'
      )
      assert.strictEqual(rows.length, 2)

      // Alice has 2 posts
      const alice = rows[0]
      assert.strictEqual(alice.name, 'Alice')
      assert.strictEqual(alice.posts.length, 2)
      const aliceTitles = alice.posts.map((p: any) => p.title).sort()
      assert.deepStrictEqual(aliceTitles, ['First Post', 'Second Post'])

      // Bob has 1 post
      const bob = rows[1]
      assert.strictEqual(bob.name, 'Bob')
      assert.strictEqual(bob.posts.length, 1)
      assert.strictEqual(bob.posts[0].title, 'Bobs Post')
    })

    it('should return empty array for entity with no related records', async () => {
      const rows = await oql.queryMany(
        'users { id name posts { id title } } [id = 3]'
      )
      assert.strictEqual(rows.length, 1)
      assert.strictEqual(rows[0].name, 'Charlie')
      assert.deepStrictEqual(rows[0].posts, [])
    })
  })
})
