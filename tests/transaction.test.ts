import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { backend, createOQL, mutationSchema, type OQL } from './setup.ts'

describe('OQL transactions', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL(mutationSchema)
  })

  after(async () => {
    await oql.raw(`DELETE FROM mut_users WHERE name LIKE 'Tx%'`)
    oql.close?.()
  })

  const countNamed = async (name: string): Promise<number> =>
    oql.count(`users [name = :name]`, { name })

  describe('commit', () => {
    it('should keep every write when the body resolves', async () => {
      await oql.transaction!(async (tx) => {
        await tx.entity('users').insert({ name: 'TxCommitOne', email: null, active: true })
        await tx.entity('users').insert({ name: 'TxCommitTwo', email: null, active: true })
      })

      assert.strictEqual(await countNamed('TxCommitOne'), 1)
      assert.strictEqual(await countNamed('TxCommitTwo'), 1)
    })

    it('should return the body result', async () => {
      const result = await oql.transaction!(async (tx) => {
        const user = await tx.entity('users').insert({ name: 'TxResult', email: null, active: true })

        return user.id
      })

      assert.strictEqual(typeof result, 'number')
    })
  })

  describe('rollback', () => {
    it('should undo every write when the body rejects', async () => {
      await assert.rejects(
        oql.transaction!(async (tx) => {
          await tx.entity('users').insert({ name: 'TxRollbackOne', email: null, active: true })
          await tx.entity('users').insert({ name: 'TxRollbackTwo', email: null, active: true })

          throw new Error('deliberate failure')
        }),
        /deliberate failure/,
      )

      assert.strictEqual(await countNamed('TxRollbackOne'), 0)
      assert.strictEqual(await countNamed('TxRollbackTwo'), 0)
    })

    it('should undo earlier writes when a later statement fails', async () => {
      await assert.rejects(
        oql.transaction!(async (tx) => {
          await tx.entity('users').insert({ name: 'TxRollbackPartial', email: null, active: true })
          // name is NOT NULL, so this insert fails in the database
          await tx.raw(`INSERT INTO mut_users (name) VALUES (NULL)`)
        }),
      )

      assert.strictEqual(await countNamed('TxRollbackPartial'), 0)
    })

    it('should leave the connection usable afterwards', async () => {
      await oql.transaction!(async (tx) => {
        await tx.entity('users').insert({ name: 'TxAfterRollback', email: null, active: true })
      })

      assert.strictEqual(await countNamed('TxAfterRollback'), 1)
    })
  })

  describe('isolation', () => {
    it('should read its own uncommitted writes', async () => {
      await oql.transaction!(async (tx) => {
        await tx.entity('users').insert({ name: 'TxOwnRead', email: null, active: true })

        assert.strictEqual(await tx.count(`users [name = 'TxOwnRead']`), 1)
      })
    })

    // petradb runs on a single session, so there is no second connection to
    // read from — the isolation this checks is a pg-backend property.
    it('should hide uncommitted writes from other connections', { skip: backend !== 'pg' }, async () => {
      await oql.transaction!(async (tx) => {
        await tx.entity('users').insert({ name: 'TxUncommitted', email: null, active: true })

        assert.strictEqual(await countNamed('TxUncommitted'), 0)
      })

      assert.strictEqual(await countNamed('TxUncommitted'), 1)
    })
  })

  describe('nesting', () => {
    it('should join the transaction it is already in', async () => {
      await assert.rejects(
        oql.transaction!(async (tx) => {
          await tx.transaction!(async (inner) => {
            await inner.entity('users').insert({ name: 'TxNested', email: null, active: true })
          })

          throw new Error('outer failure')
        }),
        /outer failure/,
      )

      assert.strictEqual(await countNamed('TxNested'), 0)
    })
  })

  describe('mutation kinds', () => {
    it('should roll back updates and deletes too', async () => {
      const user = await oql.entity('users').insert({ name: 'TxUpdated', email: null, active: true })
      const doomed = await oql.entity('users').insert({ name: 'TxDeleted', email: null, active: true })

      await assert.rejects(
        oql.transaction!(async (tx) => {
          await tx.entity('users').update(user.id, { name: 'TxRenamed' })
          await tx.entity('users').delete(doomed.id)

          throw new Error('deliberate failure')
        }),
        /deliberate failure/,
      )

      assert.strictEqual(await countNamed('TxRenamed'), 0)
      assert.strictEqual(await countNamed('TxUpdated'), 1)
      assert.strictEqual(await countNamed('TxDeleted'), 1)
    })
  })
})
