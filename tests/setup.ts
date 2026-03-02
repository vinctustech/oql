import { OQL } from '@vinctus/oql'

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

INSERT INTO posts (title, body, author) VALUES
  ('First Post', 'Hello world', 1),
  ('Second Post', 'Another post', 1),
  ('Bobs Post', 'From Bob', 2);

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

// Default test database config - matches tests/docker-compose.yml
export const dbConfig = {
  host: process.env.DB_HOST ?? 'localhost',
  port: parseInt(process.env.DB_PORT ?? '5434'),
  database: process.env.DB_NAME ?? 'postgres',
  user: process.env.DB_USER ?? 'postgres',
  password: process.env.DB_PASSWORD ?? 'docker',
}

export function createOQL(schema: string = testSchema): OQL {
  return new OQL(
    schema,
    dbConfig.host,
    dbConfig.port,
    dbConfig.database,
    dbConfig.user,
    dbConfig.password,
    false,
    10000,
    10
  )
}

export async function resetDatabase(): Promise<void> {
  const oql = createOQL()
  await oql.raw(resetSQL)
  oql.close()
}
