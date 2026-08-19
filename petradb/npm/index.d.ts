export class QueryBuilder<T = any> {

  cond(v: any): QueryBuilder<T>

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

  insert<T = any>(obj: any): Promise<T>

  link(id1: any, resource: string, id2: any): Promise<void>

  unlink(id1: any, resource: string, id2: any): Promise<void>

  update<T = any>(id: any, updates: any): Promise<T>

  bulkUpdate(updates: [any, any][]): Promise<void>

  delete(id: any): Promise<void>

  bulkDelete(id: any[]): Promise<void>

}

export type StorageType = "memory" | "persistent" | "text"

export class OQL_PETRADB {

  constructor(dm: string, storage?: StorageType, path?: string)

  create(): Promise<void>

  showQuery(): void

  entity(name: string): Mutation

  queryBuilder<T = any>(fixed?: string, at?: any): QueryBuilder<T>

  queryOne<T = any>(oql: string, parameters?: any, fixed?: string, at?: any): Promise<T | undefined>

  queryMany<T = any>(oql: string, parameters?: any, fixed?: string, at?: any): Promise<T[]>

  count(oql: string, parameters?: any, fixed?: string, at?: any): Promise<number>

  queryOneAST<T = any>(ast: any, fixed?: string, at?: any): Promise<T | undefined>

  queryManyAST<T = any>(ast: any, fixed?: string, at?: any): Promise<T[]>

  countAST(ast: any, fixed?: string, at?: any): Promise<number>

  raw<T = any>(sql: string, values?: any[]): Promise<T[]>

  transaction<T = any>(body: (tx: OQL_PETRADB) => Promise<T>): Promise<T>

  rawMulti(sql: string): Promise<void>

}
