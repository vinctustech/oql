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
CREATE TABLE "job" (
  "id" INTEGER PRIMARY KEY,
  "jobTitle" TEXT
);
CREATE TABLE "department" (
  "id" INTEGER PRIMARY KEY,
  "departmentName" TEXT
);
CREATE TABLE "employee" (
  "id" INTEGER PRIMARY KEY,
  "firstName" TEXT,
  "lastName" TEXT,
  "manager" INTEGER,
  "job" INTEGER,
  "department" INTEGER
);
ALTER TABLE "employee" ADD FOREIGN KEY ("manager") REFERENCES "employee";
ALTER TABLE "employee" ADD FOREIGN KEY ("job") REFERENCES "job";
ALTER TABLE "employee" ADD FOREIGN KEY ("department") REFERENCES "department";
INSERT INTO "job" ("id", "jobTitle") VALUES
  (4, 'President'),
  (5, 'Administration Vice President'),
  (9, 'Programmer'),
  (20, 'IT Manager');
INSERT INTO "department" ("id", "departmentName") VALUES
  (9, 'Executive'),
  (6, 'IT');
INSERT INTO "employee" ("id", "firstName", "lastName", "manager", "job", "department") VALUES
  (100, 'Steven', 'King', null, 4, 9),
  (101, 'Neena', 'Kochhar', 100, 5, 9),
  (102, 'Lex', 'De Haan', 100, 5, 9),
  (103, 'Alexander', 'Hunold', 102, 20, 6),
  (104, 'Bruce', 'Ernst', 103, 9, 6);
