CREATE TABLE "author" (
  "pk_author_id" INTEGER PRIMARY KEY,
  "name" TEXT
);
CREATE TABLE "book" (
  "pk_book_id" INTEGER PRIMARY KEY,
  "title" TEXT,
  "year" INTEGER,
  "author_id" INTEGER
);
ALTER TABLE "book" ADD FOREIGN KEY ("author_id") REFERENCES "author";
INSERT INTO "author" ("pk_author_id", "name") VALUES
  (1, 'Robert Louis Stevenson'),
  (2, 'Lewis Carroll'),
  (3, 'Charles Dickens'),
  (4, 'Mark Twain');
INSERT INTO "book" ("pk_book_id", "title", "year", "author_id") VALUES
  (1, 'Treasure Island', 1883, 1),
  (2, 'Alice''s Adventures in Wonderland', 1865, 2),
  (3, 'Oliver Twist', 1838, 3),
  (4, 'A Tale of Two Cities', 1859, 3),
  (5, 'The Adventures of Tom Sawyer', 1876, 4),
  (6, 'Adventures of Huckleberry Finn', 1884, 4);
