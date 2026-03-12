import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL_PG as OQL } from '@vinctus/oql-pg'
import { createOQL, enumSchema, enumMutationSchema } from './setup.ts'

describe('OQL enum support', () => {
  let oql: OQL
  let mutOql: OQL

  before(() => {
    oql = createOQL(enumSchema)
    mutOql = createOQL(enumMutationSchema)
  })

  after(() => {
    oql.close()
    mutOql.close()
  })

  describe('projection', () => {
    it('should return enum values as strings', async () => {
      const tickets = await oql.queryMany('tickets { title priority } <id>')
      assert.deepStrictEqual(tickets, [
        { title: 'Bug fix', priority: 'high' },
        { title: 'Feature request', priority: 'medium' },
        { title: 'Crash report', priority: 'critical' },
        { title: 'Documentation', priority: 'low' },
      ])
    })

    it('should return enum values with wildcard projection', async () => {
      const ticket = await oql.queryOne('tickets { * } [id = 1]')
      assert.deepStrictEqual(ticket, { id: 1, title: 'Bug fix', priority: 'high' })
    })
  })

  describe('filtering', () => {
    it('should filter by enum value with equals', async () => {
      const tickets = await oql.queryMany("tickets { title } [priority = 'high']")
      assert.deepStrictEqual(tickets, [{ title: 'Bug fix' }])
    })

    it('should filter by enum value with IN', async () => {
      const tickets = await oql.queryMany("tickets { title } [priority IN ('high', 'critical')] <id>")
      assert.deepStrictEqual(tickets, [
        { title: 'Bug fix' },
        { title: 'Crash report' },
      ])
    })

    it('should filter by enum value with parameter', async () => {
      const tickets = await oql.queryMany('tickets { title } [priority = :p]', { p: 'medium' })
      assert.deepStrictEqual(tickets, [{ title: 'Feature request' }])
    })
  })

  describe('ordering', () => {
    it('should order by enum column (enum-defined order)', async () => {
      const tickets = await oql.queryMany('tickets { title priority } <priority>')
      assert.deepStrictEqual(tickets, [
        { title: 'Documentation', priority: 'low' },
        { title: 'Feature request', priority: 'medium' },
        { title: 'Bug fix', priority: 'high' },
        { title: 'Crash report', priority: 'critical' },
      ])
    })

    it('should order by enum column descending', async () => {
      const tickets = await oql.queryMany('tickets { title priority } <priority DESC>')
      assert.deepStrictEqual(tickets, [
        { title: 'Crash report', priority: 'critical' },
        { title: 'Bug fix', priority: 'high' },
        { title: 'Feature request', priority: 'medium' },
        { title: 'Documentation', priority: 'low' },
      ])
    })
  })

  describe('mutations', () => {
    it('should insert a record with enum value', async () => {
      const ticket = await mutOql.entity('tickets').insert({
        title: 'New ticket',
        priority: 'low',
      })

      assert.strictEqual(typeof ticket.id, 'number')
      assert.strictEqual(ticket.title, 'New ticket')
      assert.strictEqual(ticket.priority, 'low')

      // Verify persistence
      const fetched = await mutOql.queryOne('tickets { title priority } [id = :id]', { id: ticket.id })
      assert.deepStrictEqual(fetched, { title: 'New ticket', priority: 'low' })

      await mutOql.entity('tickets').delete(ticket.id)
    })

    it('should update an enum value', async () => {
      const ticket = await mutOql.entity('tickets').insert({
        title: 'Upgrade me',
        priority: 'low',
      })

      await mutOql.entity('tickets').update(ticket.id, { priority: 'critical' })

      const fetched = await mutOql.queryOne('tickets { title priority } [id = :id]', { id: ticket.id })
      assert.deepStrictEqual(fetched, { title: 'Upgrade me', priority: 'critical' })

      await mutOql.entity('tickets').delete(ticket.id)
    })
  })
})
