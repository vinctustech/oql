import { describe, it, before, after } from 'node:test'
import assert from 'node:assert'
import { createOQL, correlatedSchema, type OQL } from './setup.ts'

// `^entity.path` references the row of the nearest enclosing query over `entity`.
// Seed: trips 1 (store North, place Depot) steps at Depot and Hotel; trip 2 (North) only at Depot;
// trip 3 (store South, place Airport) steps at Airport and Depot.
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

  it('rejects a reference to an entity no enclosing query is over', async () => {
    await assert.rejects(async () => oql.queryMany(`trips { id } [EXISTS(steps [^stores.name = 'North'])]`))
  })

  it('rejects a reference outside any subquery', async () => {
    await assert.rejects(async () => oql.queryMany(`trips { id } [^trips.ref = 'A']`))
  })
})
