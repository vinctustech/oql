import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, mutationSchema, type OQL } from './setup.ts'

describe('OQL mutations', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL(mutationSchema)
  })

  after(() => {
    oql.close?.()
  })

  describe('insert', () => {
    it('should insert a new record and return it with correct structure', async () => {
      const user = await oql.entity('users').insert({
        name: 'TestInsert',
        email: 'test@example.com',
        active: true
      })

      assert.strictEqual(typeof user.id, 'number')
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

      assert.strictEqual(typeof user.id, 'number')
      assert.deepStrictEqual(
        { name: user.name, email: user.email, active: user.active },
        { name: 'NullEmail', email: null, active: true }
      )

      // Cleanup
      await oql.entity('users').delete(user.id)
    })

    it('should insert with boolean false', async () => {
      const user = await oql.entity('users').insert({
        name: 'InactiveUser',
        email: 'inactive@example.com',
        active: false
      })

      assert.strictEqual(typeof user.id, 'number')
      assert.deepStrictEqual(
        { name: user.name, email: user.email, active: user.active },
        { name: 'InactiveUser', email: 'inactive@example.com', active: false }
      )

      // Verify persistence
      const fetched = await oql.queryOne('users { name email active } [id = :id]', { id: user.id })
      assert.deepStrictEqual(fetched, { name: 'InactiveUser', email: 'inactive@example.com', active: false })

      // Cleanup
      await oql.entity('users').delete(user.id)
    })

    it('should insert with empty string', async () => {
      const user = await oql.entity('users').insert({
        name: '',
        email: 'empty@example.com',
        active: true
      })

      assert.strictEqual(typeof user.id, 'number')
      assert.deepStrictEqual(
        { name: user.name, email: user.email, active: user.active },
        { name: '', email: 'empty@example.com', active: true }
      )

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

      assert.strictEqual(typeof post.id, 'number')
      assert.strictEqual(post.title, 'Test Post')
      assert.strictEqual(post.body, 'Test content')

      // Verify relationship
      const fetched = await oql.queryOne('posts { title body author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Test Post',
        body: 'Test content',
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

      assert.strictEqual(typeof post.id, 'number')
      assert.strictEqual(post.title, 'Orphan Post')
      assert.strictEqual(post.body, 'No author')

      // Verify null relationship
      const fetched = await oql.queryOne('posts { title body author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Orphan Post',
        body: 'No author',
        author: null
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })
  })

  describe('update', () => {
    // Note: update() returns { pk, ...updatedFields } — only the fields passed to update,
    // not the full row. This is by design (see Mutation.scala). Use a follow-up query
    // to verify both updated and unchanged fields.

    it('should update a record and return it', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'BeforeUpdate',
        email: 'before@example.com',
        active: true
      })

      const updated = await oql.entity('users').update(inserted.id, {
        name: 'AfterUpdate'
      })

      // update() returns PK + updated fields only
      assert.strictEqual(updated.id, inserted.id)
      assert.strictEqual(updated.name, 'AfterUpdate')

      // Verify full persistence: updated field changed, others unchanged
      const fetched = await oql.queryOne('users { name email active } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { name: 'AfterUpdate', email: 'before@example.com', active: true })

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

      // update() returns PK + updated fields only
      assert.strictEqual(updated.id, inserted.id)
      assert.strictEqual(updated.name, 'MultiUpdated')
      assert.strictEqual(updated.active, false)

      // Verify full persistence: both fields updated, email unchanged
      const fetched = await oql.queryOne('users { name email active } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { name: 'MultiUpdated', email: 'multi@example.com', active: false })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should update a field to null', async () => {
      const inserted = await oql.entity('users').insert({
        name: 'HasEmail',
        email: 'has@example.com',
        active: true
      })

      const updated = await oql.entity('users').update(inserted.id, {
        email: null
      })

      // update() returns PK + updated fields only
      assert.strictEqual(updated.id, inserted.id)
      assert.strictEqual(updated.email, null)

      // Verify full persistence: email nulled, others unchanged
      const fetched = await oql.queryOne('users { name email active } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { name: 'HasEmail', email: null, active: true })

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

      // update() returns PK + updated fields only
      assert.strictEqual(updated.id, inserted.id)
      assert.strictEqual(updated.active, false)

      // Verify full persistence: active changed, others unchanged
      const fetched = await oql.queryOne('users { name email active } [id = :id]', { id: inserted.id })
      assert.deepStrictEqual(fetched, { name: 'ActiveUser', email: 'active@example.com', active: false })

      // Cleanup
      await oql.entity('users').delete(inserted.id)
    })

    it('should update foreign key relationship', async () => {
      const post = await oql.entity('posts').insert({
        title: 'Alices Post',
        body: 'Content',
        author: 1 // Alice
      })

      // Update to Bob (id=2)
      await oql.entity('posts').update(post.id, {
        author: 2
      })

      // Verify new relationship and that other fields unchanged
      const fetched = await oql.queryOne('posts { title body author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Alices Post',
        body: 'Content',
        author: { name: 'Bob' }
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })

    it('should update foreign key to null', async () => {
      const post = await oql.entity('posts').insert({
        title: 'Orphaning Post',
        body: 'Content',
        author: 1
      })

      // Remove author
      await oql.entity('posts').update(post.id, {
        author: null
      })

      // Verify null relationship and that other fields unchanged
      const fetched = await oql.queryOne('posts { title body author { name } } [id = :id]', { id: post.id })
      assert.deepStrictEqual(fetched, {
        title: 'Orphaning Post',
        body: 'Content',
        author: null
      })

      // Cleanup
      await oql.entity('posts').delete(post.id)
    })
  })

  describe('delete', () => {
    it('should delete a record', async () => {
      const user = await oql.entity('users').insert({
        name: 'ToDelete',
        email: 'delete@example.com',
        active: true
      })

      await oql.entity('users').delete(user.id)

      // Verify it's gone
      const found = await oql.queryOne('users { id } [id = :id]', { id: user.id })
      assert.strictEqual(found, undefined)
    })
  })

  describe('bulkDelete', () => {
    it('should delete multiple records', async () => {
      const u1 = await oql.entity('users').insert({ name: 'BulkDel1', email: null, active: true })
      const u2 = await oql.entity('users').insert({ name: 'BulkDel2', email: null, active: true })

      // Verify they exist before deleting
      const before = await oql.queryMany('users { id } [id IN :ids]', { ids: [u1.id, u2.id] })
      assert.strictEqual(before.length, 2)

      await oql.entity('users').bulkDelete([u1.id, u2.id])

      // Verify they're gone
      const remaining = await oql.queryMany('users { id } [id IN :ids]', { ids: [u1.id, u2.id] })
      assert.deepStrictEqual(remaining, [])
    })
  })

  describe('bulkUpdate', () => {
    it('should update multiple records', async () => {
      const u1 = await oql.entity('users').insert({ name: 'Bulk1', email: 'b1@test.com', active: true })
      const u2 = await oql.entity('users').insert({ name: 'Bulk2', email: 'b2@test.com', active: true })

      await oql.entity('users').bulkUpdate([
        [u1.id, { name: 'Bulk1Updated' }],
        [u2.id, { name: 'Bulk2Updated' }],
      ])

      const fetched1 = await oql.queryOne('users { name } [id = :id]', { id: u1.id })
      assert.strictEqual(fetched1.name, 'Bulk1Updated')

      const fetched2 = await oql.queryOne('users { name } [id = :id]', { id: u2.id })
      assert.strictEqual(fetched2.name, 'Bulk2Updated')

      await oql.entity('users').bulkDelete([u1.id, u2.id])
    })

    it('should update same fields across all records', async () => {
      const u1 = await oql.entity('users').insert({ name: 'BU1', email: 'bu1@test.com', active: true })
      const u2 = await oql.entity('users').insert({ name: 'BU2', email: 'bu2@test.com', active: true })

      await oql.entity('users').bulkUpdate([
        [u1.id, { name: 'BU1Updated', active: false }],
        [u2.id, { name: 'BU2Updated', active: false }],
      ])

      const fetched1 = await oql.queryOne('users { name email active } [id = :id]', { id: u1.id })
      assert.strictEqual(fetched1.name, 'BU1Updated')
      assert.strictEqual(fetched1.email, 'bu1@test.com') // unchanged
      assert.strictEqual(fetched1.active, false)

      const fetched2 = await oql.queryOne('users { name email active } [id = :id]', { id: u2.id })
      assert.strictEqual(fetched2.name, 'BU2Updated')
      assert.strictEqual(fetched2.email, 'bu2@test.com') // unchanged
      assert.strictEqual(fetched2.active, false)

      await oql.entity('users').bulkDelete([u1.id, u2.id])
    })
  })
})
