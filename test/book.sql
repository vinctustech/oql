CREATE TABLE author (
  pk_author_id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255)
  );

CREATE TABLE book (
  pk_book_id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255),
  year INT,
  author_id BIGINT
  );

ALTER TABLE book ADD FOREIGN KEY (author_id) REFERENCES author;

INSERT INTO author (name) VALUES
  ('Robert Louis Stevenson'),
  ('Lewis Carroll'),
  ('Charles Dickens'),
  ('Mark Twain');

INSERT INTO book (title, year, author_id) VALUES
  ('Treasure Island', 1883, 1),
  ('Alice''s Adventures in Wonderland', 1865, 2),
  ('Oliver Twist', 1838, 3),
  ('A Tale of Two Cities', 1859, 3),
  ('The Adventures of Tom Sawyer', 1876, 4),
  ('Adventures of Huckleberry Finn', 1884, 4);
