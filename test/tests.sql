CREATE TABLE "author" (
  "pk_author_id" BIGINT PRIMARY KEY,
  "name" TEXT
);
CREATE TABLE "book" (
  "pk_book_id" BIGINT PRIMARY KEY,
  "title" TEXT,
  "year" INTEGER,
  "author_id" BIGINT
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
CREATE TABLE "planet" (
  "plan_id" INTEGER PRIMARY KEY,
  "name" TEXT,
  "climate" TEXT
);
CREATE TABLE "species" (
  "spec_id" INTEGER PRIMARY KEY,
  "name" TEXT,
  "lifespan" INTEGER,
  "origin" INTEGER
);
CREATE TABLE "character" (
  "char_id" INTEGER PRIMARY KEY,
  "name" TEXT,
  "home" INTEGER,
  "species" INTEGER
);
ALTER TABLE "species" ADD FOREIGN KEY ("origin") REFERENCES "planet";
ALTER TABLE "character" ADD FOREIGN KEY ("home") REFERENCES "planet";
ALTER TABLE "character" ADD FOREIGN KEY ("species") REFERENCES "species";
INSERT INTO "planet" ("plan_id", "name", "climate") VALUES
  (1, 'Earth', 'not too bad'),
  (2, 'Vulcan', 'pretty hot'),
  (3, 'Betazed', 'awesome weather'),
  (4, 'Qo''noS', 'turbulent'),
  (5, 'Turkana IV', NULL);
INSERT INTO "species" ("spec_id", "name", "lifespan", "origin") VALUES
  (1, 'Human', 71, 1),
  (2, 'Vulcan', 220, 2),
  (3, 'Betazoid', 120, 3),
  (4, 'Klingon', 150, 4);
INSERT INTO "character" ("char_id", "name", "home", "species") VALUES
  (1, 'James Tiberius Kirk', 1, 1),
  (2, 'Spock', 1, 2),
  (3, 'Deanna Troi', 1, 3),
  (4, 'Worf, Son of Mogh', NULL, 4),
  (5, 'Kurn, Son of Mogh', 4, 4),
  (6, 'Lwaxana Troi', 3, 3),
  (7, 'Natasha Yar', 5, 1);
CREATE TABLE "agent" (
  "agent_code" TEXT PRIMARY KEY,
  "agent_name" TEXT,
  "working_area" TEXT,
  "commission" NUMERIC(15, 2),
  "phone_no" TEXT
);
CREATE TABLE "customer" (
  "cust_code" TEXT PRIMARY KEY,
  "name" TEXT
);
CREATE TABLE "order" (
  "ord_num" INTEGER PRIMARY KEY,
  "ord_amount" NUMERIC(15, 2),
  "advance_amount" NUMERIC(15, 2),
  "ord_date" TEXT,
  "cust_code" TEXT,
  "agent_code" TEXT
);
ALTER TABLE "order" ADD FOREIGN KEY ("cust_code") REFERENCES "customer";
ALTER TABLE "order" ADD FOREIGN KEY ("agent_code") REFERENCES "agent";
INSERT INTO "agent" ("agent_code", "agent_name", "working_area", "commission", "phone_no") VALUES
  ('A007', 'Ramasundar', 'Bangalore', 0.15, '077-25814763'),
  ('A003', 'Alex', 'London', 0.13, '075-12458969'),
  ('A008', 'Alford', 'New York', 0.12, '044-25874365'),
  ('A011', 'Ravi Kumar', 'Bangalore', 0.15, '077-45625874'),
  ('A010', 'Santakumar', 'Chennai', 0.14, '007-22388644'),
  ('A012', 'Lucida', 'San Jose', 0.12, '044-52981425'),
  ('A005', 'Anderson', 'Brisban', 0.13, '045-21447739'),
  ('A001', 'Subbarao', 'Bangalore', 0.14, '077-12346674'),
  ('A002', 'Mukesh', 'Mumbai', 0.11, '029-12358964'),
  ('A006', 'McDen', 'London', 0.15, '078-22255588'),
  ('A004', 'Ivan', 'Torento', 0.15, '008-22544166'),
  ('A009', 'Benjamin', 'Hampshair', 0.11, '008-22536178');
INSERT INTO "customer" ("cust_code", "name") VALUES
  ('C00002', 'C00002 asdf'),
  ('C00003', 'C00003 asdf'),
  ('C00023', 'C00023 asdf'),
  ('C00007', 'C00007 asdf'),
  ('C00008', 'C00008 asdf'),
  ('C00025', 'C00025 asdf'),
  ('C00004', 'C00004 asdf'),
  ('C00021', 'C00021 asdf'),
  ('C00011', 'C00011 asdf'),
  ('C00001', 'C00001 asdf'),
  ('C00020', 'C00020 asdf'),
  ('C00006', 'C00006 asdf'),
  ('C00005', 'C00005 asdf'),
  ('C00018', 'C00018 asdf'),
  ('C00014', 'C00014 asdf'),
  ('C00022', 'C00022 asdf'),
  ('C00009', 'C00009 asdf'),
  ('C00010', 'C00010 asdf'),
  ('C00017', 'C00017 asdf'),
  ('C00024', 'C00024 asdf'),
  ('C00015', 'C00015 asdf'),
  ('C00012', 'C00012 asdf'),
  ('C00019', 'C00019 asdf'),
  ('C00016', 'C00016 asdf');
INSERT INTO "order" ("ord_num", "ord_amount", "advance_amount", "ord_date", "cust_code", "agent_code") VALUES
  (200114, 3500, 2000, '15-AUG-08', 'C00002', 'A008'),
  (200122, 2500, 400, '16-SEP-08', 'C00003', 'A004'),
  (200118, 500, 100, '20-JUL-08', 'C00023', 'A006'),
  (200119, 4000, 700, '16-SEP-08', 'C00007', 'A010'),
  (200121, 1500, 600, '23-SEP-08', 'C00008', 'A004'),
  (200130, 2500, 400, '30-JUL-08', 'C00025', 'A011'),
  (200134, 4200, 1800, '25-SEP-08', 'C00004', 'A005'),
  (200108, 4000, 600, '15-FEB-08', 'C00008', 'A004'),
  (200103, 1500, 700, '15-MAY-08', 'C00021', 'A005'),
  (200105, 2500, 500, '18-JUL-08', 'C00025', 'A011'),
  (200109, 3500, 800, '30-JUL-08', 'C00011', 'A010'),
  (200101, 3000, 1000, '15-JUL-08', 'C00001', 'A008'),
  (200111, 1000, 300, '10-JUL-08', 'C00020', 'A008'),
  (200104, 1500, 500, '13-MAR-08', 'C00006', 'A004'),
  (200106, 2500, 700, '20-APR-08', 'C00005', 'A002'),
  (200125, 2000, 600, '10-OCT-08', 'C00018', 'A005'),
  (200117, 800, 200, '20-OCT-08', 'C00014', 'A001'),
  (200123, 500, 100, '16-SEP-08', 'C00022', 'A002'),
  (200120, 500, 100, '20-JUL-08', 'C00009', 'A002'),
  (200116, 500, 100, '13-JUL-08', 'C00010', 'A009'),
  (200124, 500, 100, '20-JUN-08', 'C00017', 'A007'),
  (200126, 500, 100, '24-JUN-08', 'C00022', 'A002'),
  (200129, 2500, 500, '20-JUL-08', 'C00024', 'A006'),
  (200127, 2500, 400, '20-JUL-08', 'C00015', 'A003'),
  (200128, 3500, 1500, '20-JUL-08', 'C00009', 'A002'),
  (200135, 2000, 800, '16-SEP-08', 'C00007', 'A010'),
  (200131, 900, 150, '26-AUG-08', 'C00012', 'A012'),
  (200133, 1200, 400, '29-JUN-08', 'C00009', 'A002'),
  (200100, 1000, 600, '08-JAN-08', 'C00015', 'A003'),
  (200110, 3000, 500, '15-APR-08', 'C00019', 'A010'),
  (200107, 4500, 900, '30-AUG-08', 'C00007', 'A010'),
  (200112, 2000, 400, '30-MAY-08', 'C00016', 'A007'),
  (200113, 4000, 600, '10-JUN-08', 'C00022', 'A002'),
  (200102, 2000, 300, '25-MAY-08', 'C00012', 'A012');
CREATE TABLE "students" (
  "id" INTEGER PRIMARY KEY,
  "student_name" TEXT
);
CREATE TABLE "class" (
  "id" INTEGER PRIMARY KEY,
  "name" TEXT
);
CREATE TABLE "student_class" (
  "studentid" INTEGER,
  "classid" INTEGER,
  "year" INTEGER,
  "semester" TEXT,
  "grade" TEXT
);
ALTER TABLE "student_class" ADD FOREIGN KEY ("studentid") REFERENCES "students";
ALTER TABLE "student_class" ADD FOREIGN KEY ("classid") REFERENCES "class";
INSERT INTO "students" ("id", "student_name") VALUES
  (1, 'John'),
  (2, 'Debbie');
INSERT INTO "class" ("id", "name") VALUES
  (1, 'English'),
  (2, 'Maths'),
  (3, 'Spanish'),
  (4, 'Biology'),
  (5, 'Science'),
  (6, 'Programming'),
  (7, 'Law'),
  (8, 'Commerce'),
  (9, 'Physical Education');
INSERT INTO "student_class" ("studentid", "classid", "year", "semester", "grade") VALUES
  (1, 3, 2019, 'fall', 'B+'),
  (1, 5, 2018, 'winter', 'A'),
  (1, 9, 2019, 'summer', 'F'),
  (2, 1, 2018, 'fall', 'A+'),
  (2, 4, 2019, 'winter', 'B-'),
  (2, 5, 2018, 'summer', 'A-'),
  (2, 9, 2019, 'fall', 'B+');
CREATE TABLE "country" (
  "id" INTEGER PRIMARY KEY,
  "name" TEXT
);
CREATE TABLE "rep" (
  "id" INTEGER PRIMARY KEY,
  "name" TEXT,
  "country" INTEGER
);
ALTER TABLE "rep" ADD FOREIGN KEY ("country") REFERENCES "country";
INSERT INTO "country" ("id", "name") VALUES
  (1, 'Nigeria'),
  (2, 'Ghana'),
  (3, 'South Africa'),
  (4, 'Republic of China (Taiwan)');
INSERT INTO "rep" ("id", "name", "country") VALUES
  (1, 'Abubakar Ahmad', 1),
  (2, 'Joseph Nkrumah', 2),
  (3, 'Lauren Zuma', 3),
  (4, 'Batman', NULL);
CREATE TYPE "color" AS ENUM ('red', 'blue', 'black', 'white', 'gray', 'silver', 'green', 'yellow');
CREATE TABLE "cars" (
  "make" TEXT,
  "color" color
);
INSERT INTO "cars" ("make", "color") VALUES
  ('ferrari', 'red'),
  ('aston martin', 'blue'),
  ('bentley', 'gray'),
  ('ford', 'black');
CREATE TABLE "escapes" (
  "s" TEXT
);
INSERT INTO "escapes" ("s") VALUES
  (E'\ba\ts\nd\rf"\'\\\f'),
  (E'\u03B8');