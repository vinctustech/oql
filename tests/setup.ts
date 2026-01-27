import { OQL } from '@vinctus/oql'

// Test schema with multiple entities
export const testSchema = `
entity users {
 *id: integer
  name: text
  email: text
  active: boolean
}

entity posts {
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

-- Seed data
INSERT INTO users (name, email, active) VALUES
  ('Alice', 'alice@example.com', true),
  ('Bob', 'bob@example.com', true),
  ('Charlie', 'charlie@example.com', false);

INSERT INTO posts (title, body, author) VALUES
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
