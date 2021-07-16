CREATE TABLE account (
  id SERIAL PRIMARY KEY,
  name TEXT
  );

CREATE TABLE store (
  id SERIAL PRIMARY KEY,
  account INTEGER,
  name TEXT
  );

CREATE TABLE "user" (
  id SERIAL PRIMARY KEY,
  account INTEGER,
  email TEXT,
  password TEXT,
  name TEXT
  );

CREATE TABLE users_stores (
  account INTEGER,
  store INTEGER
  );

CREATE TABLE vehicle (
  id SERIAL PRIMARY KEY,
  driver INTEGER,
  make TEXT,
  store INTEGER
  );

CREATE TABLE customer (
  id SERIAL PRIMARY KEY,
  store INTEGER,
  name TEXT
  );

CREATE TABLE trip (
  id SERIAL PRIMARY KEY,
  state TEXT,
  customer INTEGER,
  store INTEGER,
  vehicle INTEGER
  );

--

ALTER TABLE store ADD FOREIGN KEY (account) REFERENCES account;

ALTER TABLE "user" ADD FOREIGN KEY (account) REFERENCES account;

ALTER TABLE vehicle ADD FOREIGN KEY (driver) REFERENCES "user";

ALTER TABLE vehicle ADD FOREIGN KEY (store) REFERENCES store;

ALTER TABLE customer ADD FOREIGN KEY (store) REFERENCES store;

--

INSERT INTO account (name) VALUES
  ('acc1'),
  ('acc2');

INSERT INTO store (name, account) VALUES
  ('sto1.acc1', 1),
  ('sto1.acc2', 2);

INSERT INTO vehicle (make, store) VALUES
  ('veh1.sto1.acc1', 1),
  ('veh1.sto1.acc2', 2);
