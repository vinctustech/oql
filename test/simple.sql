CREATE TABLE t (
  id SERIAL PRIMARY KEY,
  s VARCHAR(255)
  );

INSERT INTO t (s) VALUES
  ('this is\na test');
