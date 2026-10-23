export const backend = (process.env.OQL_BACKEND ?? 'pg') as 'pg' | 'petradb'

// Common OQL interface — both backends share these methods
export interface OQL {
  create(): Promise<void>
  showQuery(): void
  entity(name: string): any
  queryBuilder<T = any>(fixed?: string, at?: any): any
  queryOne<T = any>(oql: string, parameters?: any, fixed?: string, at?: any): Promise<T | undefined>
  queryMany<T = any>(oql: string, parameters?: any, fixed?: string, at?: any): Promise<T[]>
  count(oql: string, parameters?: any, fixed?: string, at?: any): Promise<number>
  // AST entry points — accept a pre-built plain-object AST, bypassing the string parser
  queryOneAST<T = any>(ast: any, fixed?: string, at?: any): Promise<T | undefined>
  queryManyAST<T = any>(ast: any, fixed?: string, at?: any): Promise<T[]>
  countAST(ast: any, fixed?: string, at?: any): Promise<number>
  raw<T = any>(sql: string, values?: any[]): Promise<T[]>
  // Only backends that support transactions define this
  transaction?<T>(body: (tx: OQL) => Promise<T>): Promise<T>
  close?: () => void
}

// Lazy-loaded backend constructors — only the selected backend is imported
let _OQL_PG: any
let _OQL_PETRADB: any

async function loadBackend() {
  if (backend === 'petradb') {
    if (!_OQL_PETRADB) {
      const mod = await import('@vinctus/oql-petradb')
      _OQL_PETRADB = mod.OQL_PETRADB
    }
  } else {
    if (!_OQL_PG) {
      const mod = await import('@vinctus/oql-pg')
      _OQL_PG = mod.OQL_PG
    }
  }
}

// Read-only schema — points at seed tables that are never modified
export const testSchema = `
entity users {
 *id: integer
  name: text
  email: text
  active: boolean
  posts: [posts].author
}

entity posts {
 *id: integer
  title: text
  body: text
  author: users
}

entity tagged {
 *id: integer
  label: text
  tags: text[]
}
`

// Mutation schema — same entities, mapped to separate tables for insert/update/delete tests
export const mutationSchema = `
entity users (mut_users) {
 *id: integer
  name: text
  email: text
  active: boolean
}

entity posts (mut_posts) {
 *id: integer
  title: text
  body: text
  author: users
}
`

// Non-integer primary key schema — exercises primary key rendering in bulk mutations
export const nonIntegerPKSchema = `
entity codes (mut_codes) {
 *code: text
  label: text
}

entity tokens (mut_tokens) {
 *token: uuid
  label: text
}
`

// Correlated schema — a step's own `trip` attribute shadows the enclosing trips query, so outer
// references must resolve against the enclosing scope rather than through that attribute
export const correlatedSchema = `
entity places (corr_places) {
 *id: integer
  name: text
}

entity stores (corr_stores) {
 *id: integer
  name: text
  place: places
  trips: [trips].store
}

entity trips (corr_trips) {
 *id: integer
  ref: text
  store: stores
  returnFor: trips
  returns: [trips].returnFor
  steps: [steps].trip
  drivers: [drivers] (assignments)
}

entity steps (corr_steps) {
 *id: integer
  trip: trips
  place: places
}

entity drivers (corr_drivers) {
 *id: integer
  name: text
  home: places
  trips: [trips] (assignments)
}

entity assignments (corr_assignments) {
  trip: trips
  driver: drivers
}
`

// Enum schema — tests enum column handling
export const enumSchema = `
enum priority { low medium high critical }

entity tickets {
 *id: integer
  title: text
  priority: priority
}
`

// Enum mutation schema — separate table for insert/update/delete tests
export const enumMutationSchema = `
enum priority { low medium high critical }

entity tickets (mut_tickets) {
 *id: integer
  title: text
  priority: priority
}
`

// SQL to reset the database before tests
const resetSQL = `
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS mut_posts;
DROP TABLE IF EXISTS mut_users;

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  active BOOLEAN DEFAULT true
);

CREATE TABLE posts (
  id SERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  body TEXT,
  author INTEGER REFERENCES users(id)
);

CREATE TABLE mut_users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  active BOOLEAN DEFAULT true
);

CREATE TABLE mut_posts (
  id SERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  body TEXT,
  author INTEGER REFERENCES mut_users(id)
);

-- Seed data for read-only tables
INSERT INTO users (name, email, active) VALUES
  ('Alice', 'alice@example.com', true),
  ('Bob', 'bob@example.com', true),
  ('Charlie', 'charlie@example.com', false);

-- Array-column test table (scalar = ANY(arrayColumn) membership)
DROP TABLE IF EXISTS tagged;

CREATE TABLE tagged (
  id SERIAL PRIMARY KEY,
  label VARCHAR(255) NOT NULL,
  tags TEXT[]
);

INSERT INTO tagged (label, tags) VALUES
  ('alice', ARRAY['admin', 'vip']),
  ('bob', ARRAY['vip']),
  ('charlie', ARRAY['standard']);

INSERT INTO posts (title, body, author) VALUES
  ('First Post', 'Hello world', 1),
  ('Second Post', 'Another post', 1),
  ('Bobs Post', 'From Bob', 2);

-- Correlated subquery test tables (outer references)
DROP TABLE IF EXISTS corr_assignments;
DROP TABLE IF EXISTS corr_drivers;
DROP TABLE IF EXISTS corr_steps;
DROP TABLE IF EXISTS corr_trips;
DROP TABLE IF EXISTS corr_stores;
DROP TABLE IF EXISTS corr_places;

CREATE TABLE corr_places (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE corr_stores (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  place INTEGER REFERENCES corr_places(id)
);

CREATE TABLE corr_trips (
  id SERIAL PRIMARY KEY,
  ref VARCHAR(255) NOT NULL,
  store INTEGER REFERENCES corr_stores(id),
  "returnFor" INTEGER REFERENCES corr_trips(id)
);

CREATE TABLE corr_steps (
  id SERIAL PRIMARY KEY,
  trip INTEGER REFERENCES corr_trips(id),
  place INTEGER REFERENCES corr_places(id)
);

CREATE TABLE corr_drivers (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  home INTEGER REFERENCES corr_places(id)
);

CREATE TABLE corr_assignments (
  trip INTEGER REFERENCES corr_trips(id),
  driver INTEGER REFERENCES corr_drivers(id)
);

INSERT INTO corr_places (name) VALUES ('Depot'), ('Airport'), ('Hotel');

INSERT INTO corr_stores (name, place) VALUES ('North', 1), ('South', 2);

INSERT INTO corr_trips (ref, store, "returnFor") VALUES ('A', 1, NULL), ('B', 1, NULL), ('C', 2, NULL), ('D', 2, 1);

INSERT INTO corr_steps (trip, place) VALUES (1, 1), (1, 3), (2, 1), (3, 2), (3, 1), (4, 2);

INSERT INTO corr_drivers (name, home) VALUES ('Dee', 1), ('Eve', 3);

INSERT INTO corr_assignments (trip, driver) VALUES (1, 1), (2, 2), (3, 2);

-- JSONB operator test table
DROP TABLE IF EXISTS jsonb_ops;

CREATE TABLE jsonb_ops (
  id SERIAL PRIMARY KEY,
  label VARCHAR(255) NOT NULL,
  data JSONB
);

INSERT INTO jsonb_ops (label, data) VALUES
  ('object', '{"role": "admin", "prefs": {"theme": "dark"}}'),
  ('array', '["tag1", "tag2", "tag3"]'),
  ('nested', '{"a": {"b": {"c": 1}}}'),
  ('mixed', '{"n": 42, "b": true, "s": "hello", "arr": [1, 2, 3]}');

-- JSON test tables (read-only + mutation)
DROP TABLE IF EXISTS json_write;
DROP TABLE IF EXISTS json_read;

CREATE TABLE json_read (
  id SERIAL PRIMARY KEY,
  label VARCHAR(255) NOT NULL,
  data JSON
);

INSERT INTO json_read (label, data) VALUES
  ('object', '{"role": "admin", "prefs": {"theme": "dark"}}'),
  ('array', '["tag1", "tag2", "tag3"]'),
  ('null_val', NULL),
  ('empty_obj', '{}'),
  ('empty_arr', '[]'),
  ('mixed_types', '{"n": 42, "b": true, "s": "hello"}');

CREATE TABLE json_write (
  id SERIAL PRIMARY KEY,
  label VARCHAR(255) NOT NULL,
  data JSON
);

-- Non-integer primary key tables (bulk mutation tests)
DROP TABLE IF EXISTS mut_codes;
DROP TABLE IF EXISTS mut_tokens;

CREATE TABLE mut_codes (
  code TEXT PRIMARY KEY,
  label VARCHAR(255)
);

CREATE TABLE mut_tokens (
  token UUID PRIMARY KEY,
  label VARCHAR(255)
);

INSERT INTO mut_codes (code, label) VALUES
  ('alpha', 'first'),
  ('beta', 'second');

INSERT INTO mut_tokens (token, label) VALUES
  ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'first'),
  ('bbbbbbbb-bbbb-4bbb-9bbb-bbbbbbbbbbbb', 'second'),
  ('11111111-1111-1111-1111-111111111111', 'third');

-- Enum test tables
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS mut_tickets CASCADE;
DROP TYPE IF EXISTS priority CASCADE;

CREATE TYPE priority AS ENUM ('low', 'medium', 'high', 'critical');

CREATE TABLE tickets (
  id SERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  priority priority NOT NULL
);

CREATE TABLE mut_tickets (
  id SERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  priority priority NOT NULL
);

INSERT INTO tickets (title, priority) VALUES
  ('Bug fix', 'high'),
  ('Feature request', 'medium'),
  ('Crash report', 'critical'),
  ('Documentation', 'low');

-- Seed data for mutation tables
INSERT INTO mut_users (name, email, active) VALUES
  ('Alice', 'alice@example.com', true),
  ('Bob', 'bob@example.com', true),
  ('Charlie', 'charlie@example.com', false);

INSERT INTO mut_posts (title, body, author) VALUES
  ('First Post', 'Hello world', 1),
  ('Second Post', 'Another post', 1),
  ('Bobs Post', 'From Bob', 2);
`

// Default test database config - matches tests/docker-compose.yml (pg backend only)
export const dbConfig = {
  host: process.env.DB_HOST ?? 'localhost',
  port: parseInt(process.env.DB_PORT ?? '5434'),
  database: process.env.DB_NAME ?? 'postgres',
  user: process.env.DB_USER ?? 'postgres',
  password: process.env.DB_PASSWORD ?? 'docker',
}

export async function createOQL(schema: string = testSchema): Promise<OQL> {
  await loadBackend()
  if (backend === 'petradb') {
    const oql = new _OQL_PETRADB(schema)
    // PetraDB is in-memory — each instance starts empty, so seed it with the test data
    await oql.rawMulti(resetSQL)
    return oql as OQL
  }
  return new _OQL_PG(
    schema,
    dbConfig.host,
    dbConfig.port,
    dbConfig.database,
    dbConfig.user,
    dbConfig.password,
    false,
    10000,
    10
  ) as OQL
}

export async function resetDatabase(): Promise<void> {
  await loadBackend()
  if (backend === 'petradb') {
    const oql = new _OQL_PETRADB(testSchema)
    await oql.rawMulti(resetSQL)
    return
  }
  const oql = await createOQL()
  await oql.raw(resetSQL)
  oql.close!()
}
