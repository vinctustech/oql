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
  (100, 'Steven', 'King', NULL, 4, 9),
  (101, 'Neena', 'Kochhar', 100, 5, 9),
  (102, 'Lex', 'De Haan', 100, 5, 9),
  (103, 'Alexander', 'Hunold', 102, 20, 6),
  (104, 'Bruce', 'Ernst', 103, 9, 6);
CREATE TABLE "event" (
  "id" UUID PRIMARY KEY,
  "what" TEXT,
  "when" TIMESTAMP WITHOUT TIME ZONE,
  "duration" DOUBLE PRECISION
);
CREATE TABLE "attendee" (
  "id" UUID PRIMARY KEY,
  "name" TEXT
);
CREATE TABLE "attendance" (
  "event" UUID,
  "attendee" UUID
);
ALTER TABLE "attendance" ADD FOREIGN KEY ("event") REFERENCES "event";
ALTER TABLE "attendance" ADD FOREIGN KEY ("attendee") REFERENCES "attendee";
INSERT INTO "event" ("id", "what", "when", "duration") VALUES
  ('797f15ab-56ba-4389-aca1-5c3c661fc9fb', 'start testing timestamps', '2021-04-21T17:42:49.943Z', 300),
  ('8aef4c68-7977-48cb-ba38-2764881d0843', 'woke up this morning', '2021-04-21T06:30:00.000Z', NULL);
INSERT INTO "attendee" ("id", "name") VALUES
  ('e8d982cd-dd19-4766-a627-ab33009bc259', 'me');
INSERT INTO "attendance" ("event", "attendee") VALUES
  ('797f15ab-56ba-4389-aca1-5c3c661fc9fb', 'e8d982cd-dd19-4766-a627-ab33009bc259'),
  ('8aef4c68-7977-48cb-ba38-2764881d0843', 'e8d982cd-dd19-4766-a627-ab33009bc259');
