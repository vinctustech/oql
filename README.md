<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [OQL](#oql)
  - [Overview](#overview)
  - [Installation](#installation)
    - [TypeScript/JavaScript](#typescriptjavascript)
    - [Scala.js](#scalajs)
  - [Usage](#usage)
  - [Tutorial](#tutorial)
  - [API](#api)
  - [Examples](#examples)
  - [Tests](#tests)
    - [Requirements](#requirements)
    - [Setup PostgreSQL](#setup-postgresql)
    - [Clone the repository](#clone-the-repository)
    - [Build the test database](#build-the-test-database)
    - [Run tests](#run-tests)
  - [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

OQL
===

![npm (scoped)](https://img.shields.io/npm/v/@vinctus/oql) ![GitHub Release Date](https://img.shields.io/github/release-date/vinctustech/oql) ![GitHub](https://img.shields.io/github/license/vinctustech/oql) ![GitHub last commit](https://img.shields.io/github/last-commit/vinctustech/oql) ![GitHub issues](https://img.shields.io/github/issues/vinctustech/oql) ![Snyk Vulnerabilities for npm scoped package](https://img.shields.io/snyk/vulnerabilities/npm/@vinctus/oql) ![npm bundle size (scoped)](https://img.shields.io/bundlephobia/minzip/@vinctus/oql) ![GitHub Workflow Status](https://img.shields.io/github/workflow/status/vinctustech/oql/unit-tests)

Object Query Language (OQL) is a simple relational database query language inspired by GraphQL and SQL, but designed to be translated query-for-query into database engine specific SQL, with identical behaviour across supported engines.

Full library documentation can be found [here](https://vinctustech.github.io/oql).

Overview
--------

*OQL* is a language for querying a relational database. The query syntax draws inspiration from GraphQL and is similar, but with many differences.  Some capabilities missing from GraphQL have been added, and some capabilities found in GraphQL have a different syntax.  We believe that much of conventional SQL syntax is still preferable to creating a completely new syntax for every single feature of the query language.  However, whereas SQL can be seen as a "low level" relational query language, OQL takes a higher level Entity-Relationship (ER) Model view of the database. 

The name "OQL" refers to both a software library, and a query language implemented within that library.  The OQL library provides support for data retrieval (via the OQL query language), and a query builder for constructing queries step by step in your code.  There are also class methods for performing all kinds of mutations, including mutations that support the ER model view of the database.  Furthermore, query and mutation operations all abide by the supplied ER data model.

Some features of the OQL language include:

- similar to [GraphQL](https://graphql.org/) in that query results have exactly the structure requested in the query (i.e., you get what you ask for).
- uses a very simple [Entity-Relationship model](https://en.wikipedia.org/wiki/Entity%E2%80%93relationship_model) description of the database 
- works with the [PostgreSQL database system](https://www.postgresql.org/)
- designed to work with existing databases without having to change the database at all

Installation
------------

### TypeScript/JavaScript

There is a [Node.js](https://nodejs.org/en/) module available through the [npm registry](https://www.npmjs.com/).

Install using the [npm install](https://docs.npmjs.com/downloading-and-installing-packages-locally) command:

```bash
npm install @vinctus/oql
```

TypeScript declarations are included in the package.

### Scala.js

There is a [Scala.js](https://www.scala-js.org/) library available through [Github Packages](https://github.com/features/packages).

Add the following lines to your `build.sbt`:

```sbt
externalResolvers += "OQL" at "https://maven.pkg.github.com/vinctustech/oql"

libraryDependencies += "com.vinctus" %%% "-vinctus-oql" % "1.1.0"

Compile / npmDependencies ++= Seq(
  "pg" -> "8.10.0",
  "@types/pg" -> "8.6.6"
  "source-map-support" -> "0.5.21",
  "big.js" -> "6.1.1",
  "@types/big.js" -> "6.1.3"
)
```

Usage
-----

Generally, using OQL in a project has the following form:

```typescript
import { OQL } from '@vinctus/oql'

const oql = new OQL(<data model>, <host>, <port>, <database>, <user>, <password>, <ssl>, <idleTimeoutMillis>, <max>)

oql.queryMany(<query>).then((<result>: any) => <handle result>)
```

where *host*, *port*, *database*, *user*, *password*, *ssl*, *idleTimeoutMillis*, and *max* are the [connection pool](https://node-postgres.com/api/pool) (`PoolConfig`) parameters for the Postgres database you are querying.

*data model* describes the parts of the database available for querying.  It's not necessary to describe every field of every table in the database, only what is being retrieved with OQL.  However, primary keys of tables that are being queried should always be included, even if you're not interested in retrieving the primary keys themselves.

*query* is the OQL query string.

*handle result* is your result array handling code.  The *result* object will be predictably structured according to the query.

Tutorial
--------

You can follow a fully explained tutorial example [here](https://vinctustech.github.io/oql/tutorial.html).

API
---

Full API documentation can be found [here](https://vinctustech.github.io/oql/api.html).

Examples
--------

Several examples are given [here](https://vinctustech.github.io/oql/examples.html).

Tests
-----

### Requirements

- Git (for cloning)
- Java 11+
- [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html) (for building and running tests)

### Setup PostgreSQL

To run the unit test, you will need to get [PostgreSQL](https://hub.docker.com/_/postgres) running in a [docker container](https://www.docker.com/resources/what-container):

```
docker pull postgres
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres
```

The [PostgreSQL client](https://www.postgresql.org/docs/13.3/app-psql.html) (`psql`) should be installed. If necessary, it can be installed with the command

`sudo apt-get install postgresql-client`

### Clone the repository

At the shell terminal go to the folder where the sources will be downloaded, referred to as `dev-path/`, and type

```
git git@github.com:vinctustech/oql.git
```

This will create folder `dev-path/oql`.

### Build the test database

Type

```shell
cd oql/test

sh start

sh tests

sh build
```

The last few lines of output should be

```
CREATE TABLE
ALTER TABLE
INSERT 0 4
INSERT 0 4
```

### Run tests

Type

```
cd ..
sbt test
```

You should see

```
[info] All tests passed.
```

License
-------

OQL uses the commercial friendly open source [ISC](https://raw.githubusercontent.com/vinctustech/oql/stable/LICENSE) license.

