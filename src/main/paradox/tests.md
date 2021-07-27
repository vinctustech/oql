Tests
=====

Requirements
------------

- Git (for cloning)
- Java 11+
- [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html) (for building and running tests)

Setup PostgreSQL
----------------

To run the unit test, you will need to get [PostgreSQL](https://hub.docker.com/_/postgres) running in a [docker container](https://www.docker.com/resources/what-container):

```
docker pull postgres
docker run --rm --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 postgres
```

The [PostgreSQL client](https://www.postgresql.org/docs/13.3/app-psql.html) (`psql`) should be installed. If necessary, it can be installed with the command

`sudo apt-get install postgresql-client`

Clone the repository
--------------------

At the shell terminal go to the folder where the sources will be downloaded, referred to as `dev-path/`, and type

```
git git@github.com:vinctustech/oql.git
```

This will create folder `dev-path/oql`.

Build the test database
-----------------------

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

Run tests
---------

Type

```
cd ..
sbt test
```

You should see

```
[info] All tests passed.
```
