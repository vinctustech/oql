import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { createOQL } from './setup.ts'

describe('OQL mutations', () => {
  let oql: OQL

  before(() => {
    oql = createOQL()
  })

  after(() => {
    oql.close()
  })

  describe('insert', () => {
    it('should insert a new record and return it with correct structure', async () => {
      const user = await oql.entity('users').insert({
        name: 'TestInsert',
        email: 'test@example.com',
        active: true
      })

      assert.ok(user.id, 'Should have an id')
      assert.deepStrictEqual(
        { name: user.name, email: user.email, active: user.active },
        { name: 'TestInsert', email: 'test@example.com', active: true }
      )

      // Cleanup
      await oql.entity('users').delete(user.id)
    })

    it('should insert with null values', async () => {
      const user = await oql.entity('users').insert({
        name: 'NullEmail',
        email: null,
        active: true
      })

      assert.ok(user.id)
      assert.strictEqual(user.email, null)

      // Cleanup
      await oql.entity('users').delete(user.id)
    })

    it('should insert with boolean false', async () => {
      const user = await oql.entity('users').insert({
        name: 'InactiveUser',
        email: 'inactive@example.com',
        active: false
      })

      assert.ok(user.id)
      assert.strictEqual(user.active, false)

      // Verify persistence
      const fetched = await oql.queryOne('users { active } [id = :id]', { id: user.id })
      assert.deepStrictEqual(fetched, { active: false })

      // Cleanup
      await oql.entity('users').delete(user.id)
    })

    it('should insert with empty string', async () => {
      const user = await oql.entity('users').insert({
        name: '',
        email: 'empty@example.com',
        active: true
      })

      assert.ok(user.id)
      assert.strictEqual(user.name, '')

      // Cleanup
      await oql.entity('users').delete(user.id)
    })

    it('should insert record with foreign key reference', async () => {
      // Insert a post referencing Alice (id=1)
      const post = await oql.entity('posts').insert({
        title: 'Test Post',
        body: 'Test content',
        author: 1
      })

      assert.ok(post.id)
      assert.strictEqual(post.title, 'Test Post')

      // Verify relationship
      const fetched = await oql.queryOne('posts { title author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Test Post',
        author: { name: 'Alice' }
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })

    it('should insert record with null foreign key', async () => {
      const post = await oql.entity('posts').insert({
        title: 'Orphan Post',
        body: 'No author',
        author: null
      })

      assert.ok(post.id)

      // Verify null relationship
      const fetched = await oql.queryOne('posts { title author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Orphan Post',
        author: null
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })
  })

  describe('update', () => {
    it('should update a record and return it', async () => {
      // Insert a test record
      const inserted = await oql.entity('users').insert({
        name: 'BeforeUpdate',
        email: 'before@example.com',
        active: true
      })

      // Update it
      const updated = await oql.entity('users').update(inserted.id, {
        name: 'AfterUpdate'
      })

      assert.strictEqual(updated.name, 'AfterUpdate')

      // Verify persistence by querying
      const fetched = await oql.queryOne('users { name email } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { name: 'AfterUpdate', email: 'before@example.com' })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should update multiple fields', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'MultiUpdate',
        email: 'multi@example.com',
        active: true
      })

      const updated = await oql.entity('users').update(inserted.id, {
        name: 'MultiUpdated',
        active: false
      })

      assert.strictEqual(updated.name, 'MultiUpdated')
      assert.strictEqual(updated.active, false)

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should update a field to null', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'HasEmail',
        email: 'has@example.com',
        active: true
      })

      // Update email to null
      const updated = await oql.entity('users').update(inserted.id, {
        email: null
      })

      assert.strictEqual(updated.email, null)

      // Verify persistence
      const fetched = await oql.queryOne('users { email } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { email: null })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should update boolean to false', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'ActiveUser',
        email: 'active@example.com',
        active: true
      })

      const updated = await oql.entity('users').update(inserted.id, {
        active: false
      })

      assert.strictEqual(updated.active, false)

      // Verify persistence
      const fetched = await oql.queryOne('users { active } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { active: false })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should update foreign key relationship', async () => {
      // Insert a post by Alice
      const post = await oql.entity('posts').insert({
        title: 'Alices Post',
        body: 'Content',
        author: 1 // Alice
      })

      // Update to Bob (id=2)
      await oql.entity('posts').update(post.id, {
        author: 2
      })

      // Verify new relationship
      const fetched = await oql.queryOne('posts { title author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Alices Post',
        author: { name: 'Bob' }
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })

    it('should update foreign key to null', async () => {
      // Insert a post by Alice
      const post = await oql.entity('posts').insert({
        title: 'Orphaning Post',
        body: 'Content',
        author: 1
      })

      // Remove author
      await oql.entity('posts').update(post.id, {
        author: null
      })

      // Verify null relationship
      const fetched = await oql.queryOne('posts { title author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Orphaning Post',
        author: null
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })
  })

  describe('delete', () => {
    it('should delete a record', async () => {
      // Insert a record to delete
      const user = await oql.entity('users').insert({
        name: 'ToDelete',
        email: 'delete@example.com',
        active: true
      })

      // Delete it
      await oql.entity('users').delete(user.id)

      // Verify it's gone
      const found = await oql.queryOne('users { id } [id = :id]', { id: user.id })
      assert.strictEqual(found, undefined)
    })
  })

  describe('bulkDelete', () => {
    it('should delete multiple records', async () => {
      // Insert records to delete
      const u1 = await oql.entity('users').insert({ name: 'BulkDel1', email: null, active: true })
      const u2 = await oql.entity('users').insert({ name: 'BulkDel2', email: null, active: true })

      // Bulk delete
      await oql.entity('users').bulkDelete([u1.id, u2.id])

      // Verify they're gone - use IN :ids (not IN (:ids)) since array renders with parens
      const remaining = await oql.queryMany('users { id } [id IN :ids]', { ids: [u1.id, u2.id] })
      assert.deepStrictEqual(remaining, [])
    })
  })
})
