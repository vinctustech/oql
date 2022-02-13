CREATE TABLE main (
  id SERIAL PRIMARY KEY,
  label TEXT
  );

CREATE TABLE "info" (
  id SERIAL PRIMARY KEY,
  main INT,
  data JSONB
  );

ALTER TABLE info ADD FOREIGN KEY (main) REFERENCES main;

INSERT INTO main (label) VALUES ('main1');

INSERT INTO "info" (main, data) VALUES
  (1, '[1, 2, {"c": 3}]'),
  (1, '{"d": [1, 2, 3]}');
