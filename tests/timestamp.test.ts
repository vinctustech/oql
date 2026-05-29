import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { backend, createOQL, type OQL } from './setup.ts'

// Regression tests for the petradb timestamp→JS Date conversion
// (PetraDBResultSet.unpack). Gated to the petradb backend: the ±2ms
// wall-clock bracket is reliable only for the in-process in-memory engine,
// not for pg running in a Docker VM with its own clock.
describe('petradb timestamps', { skip: backend !== 'petradb' }, () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL()
  })

  after(() => {
    oql.close?.()
  })

  it('CURRENT_TIMESTAMP is within a couple ms of the wall clock', async () => {
    // CURRENT_TIMESTAMP is evaluated inside the query, so it must fall within
    // the [before, after] window the query ran in (plus a 2ms slack for
    // sub-millisecond truncation). A timezone-skew bug would push it hours off.
    const before = Date.now()
    const rows = await oql.raw<{ ts: Date }>('SELECT CURRENT_TIMESTAMP AS ts')
    const after = Date.now()

    assert.strictEqual(rows.length, 1)
    const ms = new Date(rows[0].ts).getTime()
    assert.ok(
      ms >= before - 2 && ms <= after + 2,
      `CURRENT_TIMESTAMP ${new Date(ms).toISOString()} not within ` +
        `[${new Date(before - 2).toISOString()}, ${new Date(after + 2).toISOString()}]`,
    )
  })

  it('a stored UTC timestamp round-trips without a timezone shift', async () => {
    await oql.raw('DROP TABLE IF EXISTS ts_roundtrip')
    await oql.raw('CREATE TABLE ts_roundtrip (id INTEGER PRIMARY KEY, at TIMESTAMP NOT NULL)')
    await oql.raw("INSERT INTO ts_roundtrip VALUES (1, '2024-01-01T00:00:00Z')")

    const rows = await oql.raw<{ at: Date }>('SELECT at FROM ts_roundtrip WHERE id = 1')
    assert.strictEqual(rows.length, 1)
    assert.strictEqual(new Date(rows[0].at).toISOString(), '2024-01-01T00:00:00.000Z')
  })

  it('compares a timestamp column to CURRENT_TIMESTAMP (timestamp vs timestamptz)', async () => {
    // Postgres allows `timestamp <= timestamptz` via implicit cast; petradb
    // must too. CURRENT_TIMESTAMP is timestamptz, the column is timestamp.
    const ev = await createOQL('entity event { *id: integer at: timestamp }')
    await ev.raw('DROP TABLE IF EXISTS event')
    await ev.raw('CREATE TABLE event (id INTEGER PRIMARY KEY, at TIMESTAMP NOT NULL)')
    await ev.raw("INSERT INTO event VALUES (1, '2024-01-01T00:00:00Z'), (2, '2999-01-01T00:00:00')")

    const past = await ev.queryMany('event {id} [at <= CURRENT_TIMESTAMP]')
    const future = await ev.queryMany('event {id} [at > CURRENT_TIMESTAMP]')
    assert.strictEqual(past.length, 1)
    assert.strictEqual(future.length, 1)
    ev.close?.()
  })
})
