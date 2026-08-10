import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, type OQL } from './setup.ts'

// AST entry points (queryManyAST / queryOneAST / countAST) build the OQLQuery
// directly from a plain JS object, bypassing the string parser. These tests
// assert PARITY: a hand-built AST and its equivalent OQL string flow through
// the same processQuery -> queryMany pipeline, so they must return identical
// results (identical AST => identical SQL => identical rows, same order).

// Node constructors mirroring the wire contract `fromJS` consumes (kind-tagged).
const field = (name: string) => ({ kind: 'field', name })
const attr = (...ids: string[]) => ({ kind: 'attr', ids })
const int = (v: number) => ({ kind: 'int', v })
const str = (v: string) => ({ kind: 'str', v })
const bool = (v: 'TRUE' | 'FALSE' | 'NULL') => ({ kind: 'bool', v })
const infix = (op: string, left: any, right: any) => ({ kind: 'infix', op, left, right })

describe('AST entry points', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL()
  })

  after(() => {
    oql.close?.()
  })

  // Run a string query and its AST equivalent; assert identical results.
  async function parity(oqlStr: string, ast: any): Promise<any[]> {
    const fromStr = await oql.queryMany(oqlStr)
    const fromAst = await oql.queryManyAST(ast)
    assert.deepStrictEqual(fromAst, fromStr, `AST diverged from string for: ${oqlStr}`)
    return fromAst
  }

  describe('queryManyAST', () => {
    it('projection + IN list (int literals)', async () => {
      const rows = await parity('users { id name } [id IN (1,2,3)]', {
        kind: 'query',
        source: 'users',
        project: [field('id'), field('name')],
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [int(1), int(2), int(3)] },
      })
      assert.equal(rows.length, 3)
    })

    it('AND of comparison + bool literal', async () => {
      const rows = await parity('users { id name } [active = true AND id IN (1,2,3)]', {
        kind: 'query',
        source: 'users',
        project: [field('id'), field('name')],
        select: infix(
          'AND',
          infix('=', attr('active'), bool('TRUE')),
          { kind: 'in', op: 'IN', left: attr('id'), values: [int(1), int(2), int(3)] },
        ),
      })
      assert.equal(rows.length, 2) // Charlie is inactive
    })

    it('nested manyToOne relation', async () => {
      await parity('posts { id title author { id name } } [id IN (1,2,3)]', {
        kind: 'query',
        source: 'posts',
        project: [
          field('id'),
          field('title'),
          { kind: 'rel', label: 'author', source: 'author', project: [field('id'), field('name')] },
        ],
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [int(1), int(2), int(3)] },
      })
    })

    it('ordering + pagination (limit/offset)', async () => {
      await parity('users { id name } [id IN (1,2,3)] <id DESC> |2, 1|', {
        kind: 'query',
        source: 'users',
        project: [field('id'), field('name')],
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [int(1), int(2), int(3)] },
        order: [{ expr: attr('id'), dir: 'DESC' }],
        limit: 2,
        offset: 1,
      })
    })

    it('nested oneToMany relation with pagination (limit/offset)', async () => {
      // Alice (id 1) has two posts (ids 1, 2). Order DESC, limit 1 -> [{id:2}].
      const rowsLimit = await parity('users { id posts { id } <id DESC> |1| } [id = 1]', {
        kind: 'query',
        source: 'users',
        project: [
          field('id'),
          {
            kind: 'rel',
            label: 'posts',
            source: 'posts',
            project: [field('id')],
            order: [{ expr: attr('id'), dir: 'DESC' }],
            limit: 1,
          },
        ],
        select: infix('=', attr('id'), int(1)),
      })
      assert.deepStrictEqual(rowsLimit[0].posts, [{ id: 2 }])

      // Offset 1 past the DESC-ordered list -> the second post [{id:1}].
      const rowsOffset = await parity('users { id posts { id } <id DESC> |1, 1| } [id = 1]', {
        kind: 'query',
        source: 'users',
        project: [
          field('id'),
          {
            kind: 'rel',
            label: 'posts',
            source: 'posts',
            project: [field('id')],
            order: [{ expr: attr('id'), dir: 'DESC' }],
            limit: 1,
            offset: 1,
          },
        ],
        select: infix('=', attr('id'), int(1)),
      })
      assert.deepStrictEqual(rowsOffset[0].posts, [{ id: 1 }])
    })

    it('OR (grouped)', async () => {
      const rows = await parity('users { id } [(id = 1 OR id = 3)]', {
        kind: 'query',
        source: 'users',
        project: [field('id')],
        select: { kind: 'grouped', expr: infix('OR', infix('=', attr('id'), int(1)), infix('=', attr('id'), int(3))) },
      })
      assert.equal(rows.length, 2)
    })

    it('LIKE (string literal)', async () => {
      await parity("users { id name } [name LIKE 'A%']", {
        kind: 'query',
        source: 'users',
        project: [field('id'), field('name')],
        select: infix('LIKE', attr('name'), str('A%')),
      })
    })

    it('BETWEEN', async () => {
      await parity('users { id } [id BETWEEN 1 AND 2]', {
        kind: 'query',
        source: 'users',
        project: [field('id')],
        select: { kind: 'between', expr: attr('id'), lower: int(1), upper: int(2) },
      })
    })

    it('IS NULL on & reference', async () => {
      await parity('posts { id } [&author IS NULL]', {
        kind: 'query',
        source: 'posts',
        project: [field('id')],
        select: { kind: 'postfix', op: 'IS NULL', expr: { kind: 'ref', ids: ['author'] } },
      })
    })

    it('EXISTS on a relation', async () => {
      const rows = await parity('users { id name } [EXISTS(posts)]', {
        kind: 'query',
        source: 'users',
        project: [field('id'), field('name')],
        select: { kind: 'exists', source: 'posts' },
      })
      assert.equal(rows.length, 2) // Alice and Bob have posts
    })

    it('function application in an aliased projection', async () => {
      await parity('users { id up: (upper(name)) } [id = 1]', {
        kind: 'query',
        source: 'users',
        project: [
          field('id'),
          { kind: 'expr', label: 'up', expr: { kind: 'apply', f: 'upper', args: [attr('name')] } },
        ],
        select: infix('=', attr('id'), int(1)),
      })
    })

    it('default projection when none given (star)', async () => {
      await parity('users [id = 1]', {
        kind: 'query',
        source: 'users',
        select: infix('=', attr('id'), int(1)),
      })
    })

    it('CASE WHEN ... ELSE ... END in an aliased projection', async () => {
      const rows = await parity('users { id flag: (CASE WHEN active = TRUE THEN 1 ELSE 0 END) } [id IN (1,2,3)]', {
        kind: 'query',
        source: 'users',
        project: [
          field('id'),
          {
            kind: 'expr',
            label: 'flag',
            expr: {
              kind: 'case',
              whens: [{ cond: infix('=', attr('active'), bool('TRUE')), expr: int(1) }],
              els: int(0),
            },
          },
        ],
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [int(1), int(2), int(3)] },
      })
      const byId = Object.fromEntries(rows.map((r) => [r.id, r.flag]))
      assert.deepStrictEqual(byId, { 1: 1, 2: 1, 3: 0 }) // Charlie is inactive -> 0
    })

    it('CASE WHEN without ELSE (NULL on no match)', async () => {
      const rows = await parity('users { id flag: (CASE WHEN active = TRUE THEN 1 END) } [id = 3]', {
        kind: 'query',
        source: 'users',
        project: [
          field('id'),
          {
            kind: 'expr',
            label: 'flag',
            expr: {
              kind: 'case',
              whens: [{ cond: infix('=', attr('active'), bool('TRUE')), expr: int(1) }],
            },
          },
        ],
        select: infix('=', attr('id'), int(3)),
      })
      assert.equal(rows[0].flag, null) // Charlie inactive, no ELSE -> NULL
    })

    it('scalar = ANY(arrayColumn) membership', async () => {
      const rows = await parity('tagged { id } [\'vip\' = ANY(tags)]', {
        kind: 'query',
        source: 'tagged',
        project: [field('id')],
        select: { kind: 'arraycomp', left: str('vip'), op: '=', quantifier: 'ANY', array: attr('tags') },
      })
      assert.deepStrictEqual(
        rows.map((r) => r.id).sort(),
        [1, 2], // 'alice' ['admin','vip'] and 'bob' ['vip']; 'charlie' tags NULL -> excluded
      )
    })

    // Empty IN/NOT IN lists have no string-parser equivalent (the grammar
    // requires at least one element), so these assert behavior directly:
    // `x IN ()` is FALSE (matches nothing), `x NOT IN ()` is TRUE (matches all).
    it('empty IN () matches no rows', async () => {
      const rows = await oql.queryManyAST({
        kind: 'query',
        source: 'users',
        project: [field('id')],
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [] },
      })
      assert.equal(rows.length, 0)
    })

    it('empty NOT IN () matches all rows', async () => {
      const rows = await oql.queryManyAST({
        kind: 'query',
        source: 'users',
        project: [field('id')],
        select: { kind: 'in', op: 'NOT IN', left: attr('id'), values: [] },
      })
      assert.equal(rows.length, 3)
    })
  })

  describe('queryOneAST', () => {
    it('returns a single row', async () => {
      const fromStr = await oql.queryOne('users { id name } [id = 1]')
      const fromAst = await oql.queryOneAST({
        kind: 'query',
        source: 'users',
        project: [field('id'), field('name')],
        select: infix('=', attr('id'), int(1)),
      })
      assert.deepStrictEqual(fromAst, fromStr)
      assert.deepStrictEqual(fromAst, { id: 1, name: 'Alice' })
    })

    it('returns undefined on no match', async () => {
      const r = await oql.queryOneAST({
        kind: 'query',
        source: 'users',
        project: [field('id')],
        select: infix('=', attr('id'), int(-999)),
      })
      assert.equal(r, undefined)
    })
  })

  describe('countAST', () => {
    it('counts rows matching the filter', async () => {
      const fromStr = await oql.count('users [active = true]')
      const fromAst = await oql.countAST({
        kind: 'query',
        source: 'users',
        select: infix('=', attr('active'), bool('TRUE')),
      })
      assert.equal(fromAst, fromStr)
      assert.equal(fromAst, 2)
    })

    it('count ignores pagination', async () => {
      const fromAst = await oql.countAST({
        kind: 'query',
        source: 'users',
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [int(1), int(2), int(3)] },
        limit: 1,
        offset: 0,
      })
      assert.equal(fromAst, 3)
    })

    it('empty IN () counts zero; empty NOT IN () counts all', async () => {
      const none = await oql.countAST({
        kind: 'query',
        source: 'users',
        select: { kind: 'in', op: 'IN', left: attr('id'), values: [] },
      })
      assert.equal(none, 0)

      const all = await oql.countAST({
        kind: 'query',
        source: 'users',
        select: { kind: 'in', op: 'NOT IN', left: attr('id'), values: [] },
      })
      assert.equal(all, 3)
    })
  })
})
