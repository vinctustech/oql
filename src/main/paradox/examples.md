Examples
========

### Example (many-to-many)

This example presents a very simple "student" database where students are enrolled in classes, so that the students and classes are in a *many-to-many* relationship.  The example has tables and fields that are intentionally poorly named so as to demonstrate the aliasing features of the database modeling language.

Get [PostgreSQL](https://hub.docker.com/_/postgres) running in a [docker container](https://www.docker.com/resources/what-container):

```
docker pull postgres
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres
```

Run the [PostgreSQL terminal](https://www.postgresql.org/docs/13.3/app-psql.html) to create a database (with password "docker"):

`psql -h localhost -U postgres -d postgres`

which can be installed if necessary with the command

`sudo apt-get install postgresql-client`

Create a simple database by copy-pasting the following (yes, all in one shot) at the `psql` prompt:

```sql
CREATE DATABASE student;

CREATE TABLE students (
  id SERIAL PRIMARY KEY,
  stu_name TEXT
);

CREATE TABLE class (
  id SERIAL PRIMARY KEY,
  name TEXT
);

CREATE TABLE student_class (
  studentid INTEGER REFERENCES students (id),
  classid INTEGER REFERENCES class (id),
  year INTEGER,
  semester TEXT,
  grade TEXT
);

INSERT INTO students (id, stu_name) VALUES
  (1, 'John'),
  (2, 'Debbie');

INSERT INTO class (id, name) VALUES
  (1, 'English'),
  (2, 'Maths'),
  (3, 'Spanish'),
  (4, 'Biology'),
  (5, 'Science'),
  (6, 'Programming'),
  (7, 'Law'),
  (8, 'Commerce'),
  (9, 'Physical Education');

INSERT INTO student_class (studentid, classid, year, semester, grade) VALUES
  (1, 3, 2019, 'fall', 'B+'),
  (1, 5, 2018, 'winter', 'A'),
  (1, 9, 2019, 'summer', 'F'),
  (2, 1, 2018, 'fall', 'A+'),
  (2, 4, 2019, 'winter', 'B-'),
  (2, 5, 2018, 'summer', 'A-'),
  (2, 9, 2019, 'fall', 'B+');
```

The above database can be modelled using the following diagram:

![Student ER Diagram](.../er/student-er.png)

Create a file called `student-data-model` and copy-paste the following text into it:

```
entity class {
 *id: integer
  name: text
  students: [student] (enrollment)
}

entity student (students) {
 *id: integer
  name (stu_name): text
  classes: [class] (enrollment)
}

entity enrollment (student_class) {
  student (studentid): student
  class (classid): class
  year: integer
  semester: text
  grade: text
}
```

Run the following TypeScript program:

```typescript
import { OQL } from '@vinctus/oql'
import fs from 'fs'

const oql = new OQL(
  fs.readFileSync('student-data-model').toString(),
  'localhost',
  5432,
  'postgres',
  'postgres',
  'docker',
  false,
  0,
  10
)

oql
  .queryMany(
    `
    student {
      * classes { * students <name> } <name>
    } [name = 'John']
    `
  )
  .then((res: any) => console.log(JSON.stringify(res, null, 2)))
```

Output:

```json
[
  {
    "id": 1,
    "name": "John",
    "classes": [
      {
        "id": 9,
        "name": "Physical Education",
        "students": [
          {
            "id": 2,
            "name": "Debbie"
          },
          {
            "id": 1,
            "name": "John"
          }
        ]
      },
      {
        "id": 5,
        "name": "Science",
        "students": [
          {
            "id": 2,
            "name": "Debbie"
          },
          {
            "id": 1,
            "name": "John"
          }
        ]
      },
      {
        "id": 3,
        "name": "Spanish",
        "students": [
          {
            "id": 1,
            "name": "John"
          }
        ]
      }
    ]
  }
]
```

The query

```
student {
  * classes { * students <name> } <name>
} [name = 'John']
```

in the above example is asking for the names of the students enrolled only in the classes in which John is enrolled.  Also, the query is asking for the classes and the students in each class to be ordered by class name and student name, respectively.  The `*` operator is a wildcard that stands for all attributes that do not result in an array value. 

With entities that are in a many-to-many relationship, it is possible to query the junction entity that is between them.

Run the following TypeScript program:

```typescript
import { OQL } from '@vinctus/oql'
import fs from 'fs'

const oql = new OQL(
  fs.readFileSync('student-data-model').toString(),
  'localhost',
  5432,
  'postgres',
  'postgres',
  'docker',
  false,
  0,
  10
)

oql
  .queryMany(
    `
    enrollment {
      name: student.name classes: count(*)
    } /student.id/
    `
  )
  .then((res: any) => console.log(JSON.stringify(res, null, 2)))
```

Output:

```json
[
  {
    "name": "Debbie",
    "classes": 4
  },
  {
    "name": "John",
    "classes": 3
  }
]
```

The query

```
enrollment {
  name: student.name classes: count(*)
} /student.id/
```

says, "group all the students who are enrolled and show the name of each enrolled student and how many classes they are enrolled in".
