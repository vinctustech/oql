import { describe, it } from 'node:test'
import assert from 'node:assert'
import { OQL } from '@vinctus/oql'
import { createOQL, dbConfig, testSchema } from './setup.ts'

describe('OQL connection', () => {
  it('should connect with valid credentials', async () => {
    const oql = createOQL()
    // Simple query to verify connection works
    const result = await oql.raw('SELECT 1 as test')
    assert.strictEqual(result[0].test, 1)
    oql.close()
  })

  it('should fail with invalid credentials', async () => {
    const oql = new OQL(
      testSchema,
      dbConfig.host,
      dbConfig.port,
      dbConfig.database,
      'invalid_user',
      'invalid_password',
      false,
      10000,
      10
    )

    await assert.rejects(
      () => oql.raw('SELECT 1'),
      /password authentication failed|does not exist/
    )
  })

  it('should fail with invalid host', async () => {
    const oql = new OQL(
      testSchema,
      'invalid.host.example',
      dbConfig.port,
      dbConfig.database,
      dbConfig.user,
      dbConfig.password,
      false,
      1000, // Short timeout
      10
    )

    await assert.rejects(
      () => oql.raw('SELECT 1'),
      /ENOTFOUND|ETIMEDOUT|getaddrinfo/
    )
  })

  describe('close', () => {
    it('should close without error', () => {
      const oql = createOQL()
      assert.doesNotThrow(() => {
        oql.close()
      })
    })
  })
})
