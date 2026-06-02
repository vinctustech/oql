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
  })
})
