Examples
========

Here we present several examples, where each one creates a different dockerized [PostgreSQL](https://www.postgresql.org/) database.  Therefore, you will need to have [docker](https://www.docker.com/) installed.

### Example: many-to-one

This example creates a very simple employee database where employees have a manager and a department (among other things), so that the employees and their managers are in a *many-to-one* relationship.  This example also demonstrates self-referential entities.  Employees and departments are also in a *many-to-one* relationship.

Get [PostgreSQL](https://hub.docker.com/_/postgres) running in a [docker container](https://www.docker.com/resources/what-container):

```
docker pull postgres
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres
```

Run the [PostgreSQL terminal](https://www.postgresql.org/docs/9.3/app-psql.html) to create a database:

`psql -h localhost -U postgres -d postgres`

which can be installed if necessary with the command

`sudo apt-get install postgresql-client`

Create a simple database by copy-pasting the following (yes, all in one shot) at the `psql` prompt:

```sql
CREATE DATABASE employees;

CREATE TABLE department (
  dep_id SERIAL PRIMARY KEY,
  dep_name TEXT
);

CREATE TABLE employee (
  emp_id SERIAL PRIMARY KEY,
  emp_name TEXT,
  job_title TEXT,
  manager_id INTEGER REFERENCES employee,
  dep_id INTEGER REFERENCES department
);

INSERT INTO department (dep_id, dep_name) VALUES
  (1001, 'FINANCE'),             
  (2001, 'AUDIT'),
  (3001, 'MARKETING');

INSERT INTO employee (emp_id, emp_name, job_title, manager_id, dep_id) VALUES
  (68319, 'KAYLING', 'PRESIDENT', null, 1001),
  (66928, 'BLAZE', 'MANAGER', 68319, 3001),
  (67832, 'CLARE', 'MANAGER', 68319, 1001),
  (65646, 'JONAS', 'MANAGER', 68319, 2001),
  (67858, 'SCARLET', 'ANALYST', 65646, 2001),
  (69062, 'FRANK', 'ANALYST', 65646, 2001),
  (63679, 'SANDRINE', 'CLERK', 69062, 2001),
  (64989, 'ADELYN', 'SALESREP', 66928, 3001),
  (65271, 'WADE', 'SALESREP', 66928, 3001),
  (66564, 'MADDEN', 'SALESREP', 66928, 3001),
  (68454, 'TUCKER', 'SALESREP', 66928, 3001),
  (68736, 'ADNRES', 'CLERK', 67858, 2001),
  (69000, 'JULIUS', 'CLERK', 66928, 3001),
  (69324, 'MARKER', 'CLERK', 67832, 1001);
```

Create a text file called `dm` (for "data model") and copy the following text into it.

```
entity employee {
 *emp_id: integer
  name (emp_name): text
  job_title: text
  manager (manager_id): employee
  department (dep_id): department
}

entity department {
 *dep_id: integer
  name (dep_name): text
}
```

Run the following TypeScript program:

```typescript
import { OQL } from '@vinctus/oql'

const conn = new OQL("localhost", 5432, "postgres", 'postgres', 'docker', false)
const oql = 
  new OQL()

oql
  .query("employee { name manager.name department.name } [job_title = 'CLERK']", conn)
  .then((res: any) => console.log(JSON.stringify(res, null, 2)))
```

Output:

```json
[
  {
    "name": "SANDRINE",
    "manager": {
      "name": "FRANK"
    },
    "department": {
      "name": "AUDIT"
    }
  },
  {
    "name": "ADNRES",
    "manager": {
      "name": "SCARLET"
    },
    "department": {
      "name": "AUDIT"
    }
  },
  {
    "name": "JULIUS",
    "manager": {
      "name": "BLAZE"
    },
    "department": {
      "name": "MARKETING"
    }
  },
  {
    "name": "MARKER",
    "manager": {
      "name": "CLARE"
    },
    "department": {
      "name": "FINANCE"
    }
  }
]
```

The query `employee { name manager.name department.name } [job_title = 'CLERK']` in the above example program is asking for the names of employees with job title "CLERK" as well as the names of their manager and department.  In standard GraphQL, the query would have been `{ employee(job_title: 'CLERK') { name manager { name } department { name } } }`, which we feel is more verbose than it needs to be.  The use of dot notation as in `manager.name` is semantically equivalent to `manager { name }`.

### Example (many-to-many)

This example presents a very simple "student" database where students are enrolled in classes, so that the students and classes are in a *many-to-many* relationship.  The example has tables and fields that are intentionally poorly named so as to demonstrate the aliasing features of the database modeling language.

Get [PostgreSQL](https://hub.docker.com/_/postgres) running in a [docker container](https://www.docker.com/resources/what-container):

```
sudo docker pull postgres
sudo docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres
```

Run the [PostgreSQL terminal](https://www.postgresql.org/docs/9.3/app-psql.html) to create a database:

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

Run the following TypeScript program:

```typescript
import { OQL, PostgresConnection } from '@vinctus/oql'

const conn = new PostgresConnection("localhost", 5432, "postgres", 'postgres', 'docker', false)
const oql = 
  new OQL(`
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
  `)

oql
  .query("student { * classes { * students <name> } <name> } [name = 'John']", conn)
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

The query `student { * classes { * students <name> } <name> } [name = 'John']` in the above example program is asking for the names of the students enrolled only in the classes in which John is enrolled.  Also, the query is asking for the classes and the students in each class to be ordered by class name and student name, respectively.  The `*` operator is a wildcard that stands for all attributes that do not result in an array value. 
