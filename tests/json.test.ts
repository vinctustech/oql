import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { dbConfig } from './setup.ts'

const jsonReadSchema = `
entity json_read {
 *id: integer
  label: text
  data: json
}
`

const jsonWriteSchema = `
entity json_write {
 *id: integer
  label: text
  data: json
}
`

function createJsonOQL(schema: string): OQL {
  return new OQL(
    schema,
    dbConfig.host,
    dbConfig.port,
    dbConfig.database,
    dbConfig.user,
    dbConfig.password,
    false,
    10000,
    10
  )
}

describe('JSON read from JSON columns', () => {
  let oql: OQL

  before(() => {
    oql = createJsonOQL(jsonReadSchema)
  })

  after(() => {
    oql.close()
  })

  it('should read a JSON object with nested fields', async () => {
    const row = await oql.queryOne('json_read { data } [label = "object"]')
    assert.deepStrictEqual(row.data, { role: 'admin', prefs: { theme: 'dark' } })
  })

  it('should read a JSON string array', async () => {
    const row = await oql.queryOne('json_read { data } [label = "array"]')
    assert.deepStrictEqual(row.data, ['tag1', 'tag2', 'tag3'])
  })

  it('should read NULL JSON as null', async () => {
    const row = await oql.queryOne('json_read { data } [label = "null_val"]')
    assert.strictEqual(row.data, null)
  })

  it('should read empty JSON object', async () => {
    const row = await oql.queryOne('json_read { data } [label = "empty_obj"]')
    assert.deepStrictEqual(row.data, {})
  })

  it('should read empty JSON array', async () => {
    const row = await oql.queryOne('json_read { data } [label = "empty_arr"]')
    assert.deepStrictEqual(row.data, [])
  })

  it('should read mixed value types and preserve types', async () => {
    const row = await oql.queryOne('json_read { data } [label = "mixed_types"]')
    assert.deepStrictEqual(row.data, { n: 42, b: true, s: 'hello' })
    assert.strictEqual(typeof row.data.n, 'number')
    assert.strictEqual(typeof row.data.b, 'boolean')
    assert.strictEqual(typeof row.data.s, 'string')
  })

  it('should read all rows with mixed JSON types', async () => {
    const rows = await oql.queryMany('json_read { label data }')
    assert.strictEqual(rows.length, 6)
    const labels = rows.map((r: any) => r.label).sort()
    assert.deepStrictEqual(labels, ['array', 'empty_arr', 'empty_obj', 'mixed_types', 'null_val', 'object'])
  })
})

describe('JSON write to JSON columns + roundtrip', () => {
  let oql: OQL

  before(() => {
    oql = createJsonOQL(jsonWriteSchema)
  })

  after(() => {
    oql.close()
  })

  it('should insert a JSON object and read it back', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'obj_test',
      data: { key: 'value' }
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, { key: 'value' })
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert a JSON string array and read it back', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'arr_test',
      data: ['a', 'b', 'c']
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, ['a', 'b', 'c'])
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert nested JSON and read it back', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'nested_test',
      data: { a: { b: { c: 1 } } }
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, { a: { b: { c: 1 } } })
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert an empty JSON object', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'empty_obj_test',
      data: {}
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, {})
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert an empty JSON array', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'empty_arr_test',
      data: []
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, [])
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert JSON with special characters', async () => {
    const data = { quote: 'he said "hello"', newline: 'line1\nline2' }
    const row = await oql.entity('json_write').insert({
      label: 'special_chars',
      data
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, data)
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert JSON with unicode characters', async () => {
    const data = { emoji: '☺', cjk: '日本語' }
    const row = await oql.entity('json_write').insert({
      label: 'unicode_test',
      data
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, data)
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert JSON with mixed value types', async () => {
    const data = { n: 42, b: true, s: 'hello' }
    const row = await oql.entity('json_write').insert({
      label: 'mixed_types_test',
      data
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, data)
    assert.strictEqual(typeof fetched.data.n, 'number')
    assert.strictEqual(typeof fetched.data.b, 'boolean')
    assert.strictEqual(typeof fetched.data.s, 'string')
    await oql.entity('json_write').delete(row.id)
  })

  it('should update a JSON field', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'update_test',
      data: { original: true }
    })
    await oql.entity('json_write').update(row.id, {
      data: { updated: true, version: 2 }
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, { updated: true, version: 2 })
    await oql.entity('json_write').delete(row.id)
  })

  it('should update a JSON field to null', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'null_update_test',
      data: { something: 'here' }
    })
    await oql.entity('json_write').update(row.id, {
      data: null
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.strictEqual(fetched.data, null)
    await oql.entity('json_write').delete(row.id)
  })

  it('should update a JSON field from object to array', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'type_change_test',
      data: { was: 'object' }
    })
    await oql.entity('json_write').update(row.id, {
      data: ['now', 'array']
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, ['now', 'array'])
    await oql.entity('json_write').delete(row.id)
  })
})
