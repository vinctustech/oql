export class QueryBuilder {

  cond(v: any): QueryBuilder

//   add(attribute: QueryBuilder): QueryBuilder
//
//   add(query: String): QueryBuilder
//
//   project(resource: string, ...attributes: string[]): QueryBuilder

  query(oql: string): QueryBuilder

  select(oql: string): QueryBuilder

  order(attribute: string, sorting: string): QueryBuilder

  limit(a: number): QueryBuilder

  offset(a: number): QueryBuilder

  getOne(parameters?: any): Promise<any | undefined>

  getMany(parameters?: any): Promise<any[]>

  getCount(parameters?: any): Promise<number>

}

export class OQL {

  constructor(dm: string, host: string, port: number, database: string, user: string, password: string, ssl: boolean, idleTimeoutMillis: number, max: number)

//   trace: boolean
//
//   entity(resource: string): Resource

  queryBuilder(): QueryBuilder

  queryOne(oql: string, parameters?: any): Promise<any | undefined>

  queryMany(oql: string, parameters?: any): Promise<any[]>

  count(oql: string, parameters?: any): Promise<number>

  raw(sql: string, values?: any[]): Promise<any[]>

}
