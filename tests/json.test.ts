import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, type OQL } from './setup.ts'

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

const jsonbOpsSchema = `
entity jsonb_ops {
 *id: integer
  label: text
  data: json
}
`

describe('JSON read from JSON columns', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL(jsonReadSchema)
  })

  after(() => {
    oql.close?.()
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

  before(async () => {
    oql = await createOQL(jsonWriteSchema)
  })

  after(() => {
    oql.close?.()
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

  it('should insert JSON with apostrophes and read it back', async () => {
    const data = { lastName: "O'Brien-Smith", note: "it's a driver's name" }
    const row = await oql.entity('json_write').insert({
      label: 'apostrophe_test',
      data
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, data)
    await oql.entity('json_write').delete(row.id)
  })

  it('should insert JSON with backslashes and read it back', async () => {
    const data = { path: 'C:\\temp\\file', mixed: "quote ' and backslash \\" }
    const row = await oql.entity('json_write').insert({
      label: 'backslash_test',
      data
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, data)
    await oql.entity('json_write').delete(row.id)
  })

  it('should treat SQL injection attempts in JSON values as data', async () => {
    const data = { note: "'); DROP TABLE json_write; --" }
    const row = await oql.entity('json_write').insert({
      label: 'injection_test',
      data
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, data)
    await oql.entity('json_write').delete(row.id)
    const stillThere = await oql.queryMany('json_write { id }')
    assert.strictEqual(stillThere.length, 0)
  })

  it('should update a JSON field with apostrophes', async () => {
    const row = await oql.entity('json_write').insert({
      label: 'apostrophe_update_test',
      data: { name: 'plain' }
    })
    await oql.entity('json_write').update(row.id, {
      data: { name: "O'Brien-Smith" }
    })
    const fetched = await oql.queryOne('json_write { data } [id = :id]', { id: row.id })
    assert.deepStrictEqual(fetched.data, { name: "O'Brien-Smith" })
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

describe('JSONB operators', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL(jsonbOpsSchema)
  })

  after(() => {
    oql.close?.()
  })

  // Field access operators (-> and ->>)

  it('-> should return JSON value for a key', async () => {
    const row = await oql.queryOne('jsonb_ops { role: (data -> \'role\') } [label = \'object\']')
    assert.strictEqual(row.role, 'admin')
  })

  it('->> should return text value for a key', async () => {
    const row = await oql.queryOne('jsonb_ops { role: (data ->> \'role\') } [label = \'object\']')
    assert.strictEqual(row.role, 'admin')
  })

  it('-> chained should access nested JSON', async () => {
    const row = await oql.queryOne('jsonb_ops { theme: (data -> \'prefs\' -> \'theme\') } [label = \'object\']')
    assert.strictEqual(row.theme, 'dark')
  })

  it('-> then ->> should return nested value as text', async () => {
    const row = await oql.queryOne('jsonb_ops { theme: (data -> \'prefs\' ->> \'theme\') } [label = \'object\']')
    assert.strictEqual(row.theme, 'dark')
  })

  it('-> with integer index on array', async () => {
    const row = await oql.queryOne('jsonb_ops { first: (data -> 0) } [label = \'array\']')
    assert.strictEqual(row.first, 'tag1')
  })

  it('->> with integer index on array', async () => {
    const row = await oql.queryOne('jsonb_ops { first: (data ->> 0) } [label = \'array\']')
    assert.strictEqual(row.first, 'tag1')
  })

  // Path access operators (#> and #>>)

  it('#> should access nested path returning JSON', async () => {
    const row = await oql.queryOne("jsonb_ops { val: (data #> '{a,b}') } [label = 'nested']")
    assert.deepStrictEqual(row.val, { c: 1 })
  })

  it('#>> should access nested path returning text', async () => {
    const row = await oql.queryOne("jsonb_ops { val: (data #>> '{a,b,c}') } [label = 'nested']")
    assert.strictEqual(row.val, '1')
  })

  // Containment operators (@> and <@)

  it('@> should filter rows containing given JSON', async () => {
    const rows = await oql.queryMany("jsonb_ops { label } [data @> '{\"role\": \"admin\"}']")
    assert.strictEqual(rows.length, 1)
    assert.strictEqual(rows[0].label, 'object')
  })

  it('<@ should filter rows contained by given JSON', async () => {
    const rows = await oql.queryMany("jsonb_ops { label } ['{\"role\": \"admin\", \"prefs\": {\"theme\": \"dark\"}}' <@ data]")
    assert.strictEqual(rows.length, 1)
    assert.strictEqual(rows[0].label, 'object')
  })

  // Key existence (?)

  it('? should filter rows where key exists', async () => {
    const rows = await oql.queryMany("jsonb_ops { label } [data ? 'role']")
    const labels = rows.map((r: any) => r.label).sort()
    assert.deepStrictEqual(labels, ['object'])
  })

  // Operators in WHERE with other conditions

  it('->> in comparison should work in WHERE', async () => {
    const rows = await oql.queryMany("jsonb_ops { label } [data ->> 'role' = 'admin']")
    assert.strictEqual(rows.length, 1)
    assert.strictEqual(rows[0].label, 'object')
  })

  it('combined ->> conditions with AND', async () => {
    const rows = await oql.queryMany("jsonb_ops { label } [data ->> 'role' = 'admin' AND data -> 'prefs' ->> 'theme' = 'dark']")
    assert.strictEqual(rows.length, 1)
    assert.strictEqual(rows[0].label, 'object')
  })

  // Edge cases

  it('-> on missing key should return null', async () => {
    const row = await oql.queryOne("jsonb_ops { val: (data -> 'missing') } [label = 'object']")
    assert.strictEqual(row.val, null)
  })

  it('->> on numeric value should return text', async () => {
    const row = await oql.queryOne("jsonb_ops { val: (data ->> 'n') } [label = 'mixed']")
    assert.strictEqual(row.val, '42')
  })
})
