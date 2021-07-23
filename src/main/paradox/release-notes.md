Release Notes
=============

1.0.0-RC.4.2
------------

- fixed the handling of timestamp values in inner query array results

1.0.0-RC.4.1
------------

- fixed bulk update to work with primary keys that have type UUID.

1.0.0-RC.4
----------

- added mutator method `bulkUpdate()` to use PostgreSQL's extended `UPDATE` command for doing bulk updates.

1.0.0-RC.3.25
-------------

- fix `CASE` construction rendering to SQL
- fix function type resolution

1.0.0-RC.3.24
-------------

This is the third feature-complete release candidate.

Changes include:

- added NULL keyword to parser
- removed escaping quotes by doubling
- other very minor parser fixes

1.0.0-RC.3.23
-------------

- second feature-complete release candidate
- fixed a regression: escaping an apostrophe

1.0.0-RC.3.22
-------------

- first feature-complete release candidate