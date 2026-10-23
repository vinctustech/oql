import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, correlatedSchema, type OQL } from './setup.ts'

// `^entity.path` references the row of the nearest enclosing query over `entity`.
// Seed: stores North (Depot) and South (Airport). Trips: 1 'A' North, steps Depot and Hotel;
// 2 'B' North, step Depot; 3 'C' South, steps Airport and Depot; 4 'D' South, a return trip for 1,
// step Airport. Drivers: Dee (home Depot) on trip 1, Eve (home Hotel) on trips 2 and 3.
describe('Outer references', () => {
  let oql: OQL

  before(async () => {
    oql = await createOQL(correlatedSchema)
  })

  after(() => {
    oql.close?.()
  })

  async function capturedSQL(run: () => Promise<unknown>): Promise<string> {
    const lines: string[] = []
    const log = console.log

    console.log = (...args: unknown[]) => lines.push(args.join(' '))

    try {
      oql.showQuery()
      await run()
    } finally {
      console.log = log
    }

    return lines.join('\n')
  }

  it('compares a subquery row against the enclosing row through a many-to-one path', async () => {
    const rows = await oql.queryMany('trips { id } [EXISTS(steps [place.id != ^trips.store.place.id])] <id>')

    assert.deepStrictEqual(rows, [{ id: 1 }, { id: 3 }])
  })

  it('joins the outer path on the enclosing query instead of re-joining it inside the subquery', async () => {
    const sql = await capturedSQL(() =>
      oql.queryMany('trips { id } [EXISTS(steps [place.id != ^trips.store.place.id])] <id>'),
    )

    assert.match(sql, /LEFT JOIN "corr_stores" AS "corr_trips\$store"/)
    assert.match(sql, /!= "corr_trips\$store\$place"\."id"/)
    assert.doesNotMatch(sql, /"corr_trips\$steps\$trip"/)
  })

  it('reaches an attribute the subquery entity does not have', async () => {
    const rows = await oql.queryMany(`trips { id } [EXISTS(steps [^trips.ref = 'A'])] <id>`)

    assert.deepStrictEqual(rows, [{ id: 1 }])
  })

  it('resolves past an enclosing many-to-one projection', async () => {
    const rows = await oql.queryMany(
      'steps { id trip { id same: steps { id } [place.id = ^steps.place.id] <id> } } [id = 5]',
    )

    assert.deepStrictEqual(rows, [{ id: 5, trip: { id: 3, same: [{ id: 5 }] } }])
  })

  it('works through the AST entry point', async () => {
    const rows = await oql.queryManyAST({
      kind: 'query',
      source: 'trips',
      project: [{ kind: 'field', name: 'id' }],
      select: {
        kind: 'exists',
        source: 'steps',
        select: {
          kind: 'infix',
          op: '!=',
          left: { kind: 'attr', ids: ['place', 'id'] },
          right: { kind: 'outer', ids: ['trips', 'store', 'place', 'id'] },
        },
      },
      order: [{ expr: { kind: 'attr', ids: ['id'] }, dir: 'ASC' }],
    })

    assert.deepStrictEqual(rows, [{ id: 1 }, { id: 3 }])
  })

  it('compares against the enclosing row from a many-to-many EXISTS', async () => {
    const rows = await oql.queryMany('trips { id } [EXISTS(drivers [home.id = ^trips.store.place.id])] <id>')

    assert.deepStrictEqual(rows, [{ id: 1 }])
  })

  it('filters a many-to-many projection against the enclosing row', async () => {
    const rows = await oql.queryMany(
      'trips { id crew: drivers { name } [home.id != ^trips.store.place.id] <name> } [id IN (1, 2)] <id>',
    )

    assert.deepStrictEqual(rows, [
      { id: 1, crew: [] },
      { id: 2, crew: [{ name: 'Eve' }] },
    ])
  })

  it('filters a one-to-many projection against its direct parent', async () => {
    const rows = await oql.queryMany(
      'trips { id away: steps { id } [place.id != ^trips.store.place.id] <id> } [id IN (1, 2)] <id>',
    )

    assert.deepStrictEqual(rows, [
      { id: 1, away: [{ id: 2 }] },
      { id: 2, away: [] },
    ])
  })

  it('reaches a grandparent from a nested EXISTS', async () => {
    const sql = await capturedSQL(async () => {
      const rows = await oql.queryMany(
        `stores { id } [EXISTS(trips [EXISTS(steps [place.id = ^stores.place.id AND ^trips.ref = 'C'])])] <id>`,
      )

      assert.deepStrictEqual(rows, [{ id: 2 }])
    })

    assert.match(sql, /= "corr_stores\$place"\."id"/)
    assert.match(sql, /"corr_stores\$trips"\."ref" = 'C'/)
  })

  it('resolves to the nearest enclosing query when the entity encloses twice', async () => {
    // Trip 1's return trip 4 belongs to South (Airport) and stops at Airport, so the innermost
    // `^trips` must be the return trip; resolved to trip 1 (North, Depot) nothing would match.
    const rows = await oql.queryMany(
      'trips { id } [EXISTS(returns [EXISTS(steps [place.id = ^trips.store.place.id])])] <id>',
    )

    assert.deepStrictEqual(rows, [{ id: 1 }])
  })

  it('works in a scalar subquery', async () => {
    const rows = await oql.queryMany(
      'trips { id } [(steps { count(*) } [place.id != ^trips.store.place.id]) > 0] <id>',
    )

    assert.deepStrictEqual(rows, [{ id: 1 }, { id: 3 }])
  })

  it('works in an IN subquery', async () => {
    const rows = await oql.queryMany(`trips { id } [3 IN (steps { &place } [^trips.ref = 'A'])] <id>`)

    assert.deepStrictEqual(rows, [{ id: 1 }])
  })

  it('works in a projected value inside a subquery', async () => {
    const rows = await oql.queryMany(
      'trips { id steps { id atStore: (place.id = ^trips.store.place.id) } <id> } [id = 1]',
    )

    assert.deepStrictEqual(rows, [
      {
        id: 1,
        steps: [
          { id: 1, atStore: true },
          { id: 2, atStore: false },
        ],
      },
    ])
  })

  it('works in a subquery ordering', async () => {
    const rows = await oql.queryMany(
      'trips { id steps { id } <CASE WHEN place.id = ^trips.store.place.id THEN 1 ELSE 0 END, id> } [id = 3]',
    )

    assert.deepStrictEqual(rows, [{ id: 3, steps: [{ id: 5 }, { id: 4 }] }])
  })

  it('works in count and countAST', async () => {
    const count = await oql.count('trips [EXISTS(steps [place.id != ^trips.store.place.id])]')
    const countAST = await oql.countAST({
      kind: 'query',
      source: 'trips',
      project: [],
      select: {
        kind: 'exists',
        source: 'steps',
        select: {
          kind: 'infix',
          op: '!=',
          left: { kind: 'attr', ids: ['place', 'id'] },
          right: { kind: 'outer', ids: ['trips', 'store', 'place', 'id'] },
        },
      },
    })

    assert.strictEqual(count, 2)
    assert.strictEqual(countAST, 2)
  })

  it('rejects a reference to an entity no enclosing query is over', async () => {
    await assert.rejects(async () => oql.queryMany(`trips { id } [EXISTS(steps [^stores.name = 'North'])]`))
  })

  it('rejects a reference outside any subquery', async () => {
    await assert.rejects(async () => oql.queryMany(`trips { id } [^trips.ref = 'A']`))
  })
})
