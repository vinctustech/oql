export class QueryBuilder {

  cond(v: any): QueryBuilder

//   add(attribute: QueryBuilder): QueryBuilder
//
//   add(query: String): QueryBuilder
//
//   project(resource: string, ...attributes: string[]): QueryBuilder

  query(oql: string, parameters?: any): QueryBuilder

  select(oql: string, parameters?: any): QueryBuilder

  order(attribute: string, sorting: string): QueryBuilder

  limit(a: number): QueryBuilder

  offset(a: number): QueryBuilder

  getOne(): Promise<any | undefined>

  getMany(): Promise<any[]>

  getCount(): Promise<number>

}

export class Mutation {

  insert(obj: any): Promise<any>

  link(id1: any, resource: string, id2: any): Promise<void>

  unlink(id1: any, resource: string, id2: any): Promise<void>

  update(id: any, updates: any): Promise<void>

  bulkUpdate(updates: [any, any][]): Promise<void>

  delete(id: any): Promise<void>

}

export class OQL {

  constructor(dm: string, host: string, port: number, database: string, user: string, password: string, ssl: any, idleTimeoutMillis: number, max: number)

  showQuery(): void

  entity(name: string): Mutation

  queryBuilder(): QueryBuilder

  queryOne(oql: string, parameters?: any): Promise<any | undefined>

  queryMany(oql: string, parameters?: any): Promise<any[]>

  count(oql: string, parameters?: any): Promise<number>

  raw(sql: string, values?: any[]): Promise<any[]>

}
