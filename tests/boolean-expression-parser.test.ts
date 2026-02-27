import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { createOQL } from './setup.ts'

describe('boolean expression parser - standalone non-comparison expressions', () => {
  let oql: OQL

  before(() => {
    oql = createOQL()
  })

  after(() => {
    oql.close()
  })

  // All of these tests demonstrate valid boolean expressions that the parser
  // rejects because booleanPrimary only falls back to qualifiedAttributeExpression
  // and booleanLiteral — it does not include applyExpression, caseExpression,
  // or castExpression as standalone boolean values.

  describe('function call as standalone boolean', () => {
    it('should accept a function call as a boolean condition', async () => {
      // coalesce(active, true) returns boolean, valid in SQL WHERE clause
      // Fails: applyExpression is not reachable from booleanPrimary
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [coalesce(active, true)]')
      )
    })

    it('should accept NOT function_call()', async () => {
      // NOT coalesce(active, false) — NOT goes to booleanPrimary, same gap
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [NOT coalesce(active, false)]')
      )
    })

    it('should accept function call on one side of OR', async () => {
      // The right-hand side of OR must be a booleanPrimary
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [id = 1 OR coalesce(active, true)]')
      )
    })

    it('should accept function call on one side of AND', async () => {
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [id > 0 AND coalesce(active, true)]')
      )
    })
  })

  describe('CASE expression as standalone boolean', () => {
    it('should accept CASE returning boolean as a condition', async () => {
      // CASE WHEN ... THEN true ELSE false END is a valid boolean expression
      // Fails: caseExpression is not reachable from booleanPrimary
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [CASE WHEN id = 1 THEN true ELSE false END]')
      )
    })
  })

  describe('cast expression as standalone boolean', () => {
    it('should accept a cast to boolean as a condition', async () => {
      // active::boolean is valid (redundant but legal)
      // Fails: castExpression is not reachable from booleanPrimary
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [active::boolean]')
      )
    })
  })

  describe('parenthesized expression in boolean context', () => {
    it('should accept a parenthesized function call as boolean', async () => {
      // booleanPrimary's grouped form uses booleanExpression, not expression,
      // so (coalesce(active, true)) can't be parsed either
      await assert.doesNotReject(
        () => oql.queryMany('users { id name } [(coalesce(active, true))]')
      )
    })
  })
})
