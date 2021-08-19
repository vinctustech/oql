export class QueryBuilder<T> {

  cond(v: any): QueryBuilder<T>

//   add(attribute: QueryBuilder): QueryBuilder
//
//   add(query: String): QueryBuilder
//
//   project(resource: string, ...attributes: string[]): QueryBuilder

  query(oql: string, parameters?: any): QueryBuilder<T>

  select(oql: string, parameters?: any): QueryBuilder<T>

  order(attribute: string, sorting: string): QueryBuilder<T>

  limit(a: number): QueryBuilder<T>

  offset(a: number): QueryBuilder<T>

  getOne(): Promise<T | undefined>

  getMany(): Promise<T[]>

  getCount(): Promise<number>

}

export class Mutation {

  insert<T>(obj: any): Promise<T>

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

  queryBuilder<T>(fixed?: string, at?: any): QueryBuilder<T>

  queryOne<T>(oql: string, parameters?: any, fixed?: string, at?: any): Promise<T | undefined>

  queryMany<T>(oql: string, parameters?: any, fixed?: string, at?: any): Promise<T[]>

  count(oql: string, parameters?: any, fixed?: string, at?: any): Promise<number>

  raw<T>(sql: string, values?: any[]): Promise<T[]>

}
