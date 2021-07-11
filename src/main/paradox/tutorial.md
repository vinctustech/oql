Tutorial
========

We present a fully explained tutorial example that creates a dockerized [PostgreSQL](https://www.postgresql.org/) database.  Therefore, you will need to have [docker](https://www.docker.com/) installed.

See @ref:[Examples](examples.md) for other examples.

This example creates a very simple employee database where employees have a manager and a department (among other things), so that the employees and their managers are in a *many-to-one* relationship.  This example also demonstrates self-referential entities.  Employees and departments are also in a *many-to-one* relationship.  The database definition purposely contains a few oddities so that certain OQL features can be demonstrated.

Setup PostgreSQL
----------------

We need to get [PostgreSQL](https://hub.docker.com/_/postgres) running in a [docker container](https://www.docker.com/resources/what-container):

```
docker pull postgres
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres
```

The [PostgreSQL client](https://www.postgresql.org/docs/13.3/app-psql.html) (`psql`) should be installed. If necessary, it can be installed with the command

`sudo apt-get install postgresql-client`

Create the database
-------------------

Run `psql` with the command:

`psql -h localhost -U postgres -d postgres`

Enter password `docker`.

Create a simple database by copy-pasting the following (yes, all in one shot) at the `psql` prompt:

```sql
CREATE DATABASE employees;

CREATE TABLE department (
  dep_id SERIAL PRIMARY KEY,
  dep_name TEXT
);

CREATE TABLE employees (
  emp_id SERIAL PRIMARY KEY,
  emp_name TEXT,
  job_title TEXT,
  manager_id INTEGER REFERENCES employees,
  dep_id INTEGER REFERENCES department
);

INSERT INTO department (dep_id, dep_name) VALUES
  (1001, 'FINANCE'),             
  (2001, 'AUDIT'),
  (3001, 'MARKETING');

INSERT INTO employees (emp_id, emp_name, job_title, manager_id, dep_id) VALUES
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
  (68736, 'ANDRES', 'CLERK', 67858, 2001),
  (69000, 'JULIUS', 'CLERK', 66928, 3001),
  (69324, 'MARKER', 'CLERK', 67832, 1001);
```

Create the data model
---------------------

Create a text file called `data-model` and copy the following text into it.

```
entity employee (employees) {
 *emp_id: integer
  name (emp_name): text
  job_title: text
  manager (manager_id): employee
  department (dep_id): department
}

entity department {
 *dep_id: integer
  name (dep_name): text
  employees: [employee]
}
```

The data model describes the parts of the database available for querying. It’s not necessary to describe every field of every table in the database, only what is being retrieved with OQL. However, primary keys of tables that are being queried should always be included, even if you’re not interested in retrieving the primary keys themselves.

We'd like to look at the data model in some detail to explain what's going on. If you'd rather skip ahead and get on with the tutorial, then go to @ref:[Many-to-one Query](#many-to-one-query).

The above data model describes to OQL the database that we just created. There's an entity definition corresponding to each table in the database.  Each entity has an attribute definition corresponding to each column in the corresponding table, and in OQL it's possible to have attributes that don't correspond to any declared column.

The first line of the data model

```
entity employee (employees) {
```

means that the entity `employee` corresponds to table `employees` in the actual database.  This means that table names in an SQL database can be aliased and frequently are, but it's optional. 

The second line

```
 *emp_id: integer
```

means that `emp_id` is the primary key, signified by the `*`, in the corresponding table, and that it has an 32-bit integer type.

The third line

```
  name (emp_name): text
```

says that the table has a column called `emp_name` with a variable character string type, but that we want `name` to be the alias.  Attribute aliasing is optional, but is very frequently done.

Line five

```
  manager (manager_id): employee
```

says that the table has a column called `manager_id` which is a foreign key referencing the table corresponding to entity `employee`, and we would like `manager` to be the alias for that attribute.  This attribute is said to have an "entity" type, and puts the `employee` entity in a *many-to-one* relationship with itself.

The sixth line

```
  department (dep_id): department
```

means that entity `employee` is also in a *many-to-one* relationship with entity `department` (since `dep_id` is a foreign key).

@@@ note
Entities can be defined in any order. In the above attribute definition, entity `department` is being referenced even thought its definition comes after.
@@@

On line twelve within the `department` entity definition we find

```
  employees: [employee]
```

which defines an attribute called `employees` with the "entity array" type `[employee]`. This attribute doesn't correspond to any column in the table, but rather asserts the *one-to-many* relationship that entity `department` has to entity `employee`, and facilitates queries to `department` that presuppose that the relationship exists.

@@@ note
OQL checks the internal correctness of the entire data model. Specifically, whether entity array type attributes have a corresponding referencing entity type attribute.
@@@

Many-to-one Query
-----------------

Run the following TypeScript program:

```typescript
import { OQL } from '@vinctus/oql'
import fs from 'fs'

const oql = new OQL(
  fs.readFileSync('data-model').toString(),
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
    employee { 
      name manager: manager.name department { name }
    } [job_title = 'CLERK']`,
    oql
  )
  .then((res: any) => console.log(JSON.stringify(res, null, 2)))
```

Output:

```json
[
  {
    "name": "JULIUS",
    "manager": "BLAZE",
    "department": {
      "name": "MARKETING"
    }
  },
  {
    "name": "MARKER",
    "manager": "CLARE",
    "department": {
      "name": "FINANCE"
    }
  },
  {
    "name": "ANDRES",
    "manager": "SCARLET",
    "department": {
      "name": "AUDIT"
    }
  },
  {
    "name": "SANDRINE",
    "manager": "FRANK",
    "department": {
      "name": "AUDIT"
    }
  }
]
```

The query `employee { name manager: manager.name department { name } } [job_title = 'CLERK']` in the above example program is asking for the names of employees with job title "CLERK" as well as the names of their manager and department.  The query is sort-of unnatural because we're asking for the names of the manager and department in two different ways in order to demonstrate different features of OQL.

In the above query, `manager: manager.name` in the projection (i.e., what's between the `{` ... `}` after the entity you're querying) says that we want to get just the string value of the name of the employee's manager, and we want the associated property name in the result object to be `manager`.  Whereas, `department { name }` says that we want a result object corresponding to the department, with implied property name `department`, but we only want the `name` property, excluding the `dep_id` property, which we would also have gotten had we simply written `department` without the `{ name }` after it.
