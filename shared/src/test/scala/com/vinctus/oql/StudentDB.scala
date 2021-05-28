package com.vinctus.oql

trait StudentDB extends Test {

  val dm: String =
    """
      |entity class {
      | *id: integer
      |  name: text
      |  students: [student] (enrollment)
      |}
      |
      |entity student (students) {
      | *id: integer
      |  name (student_name): text
      |  classes: [class] (enrollment)
      |}
      |
      |entity enrollment (student_class) {
      |  student (studentid): student!
      |  class (classid): class!
      |  year: integer
      |  semester: text
      |  grade: text
      |}
      |""".stripMargin
  val db = new AbstractOQL(dm, ds)
  val data: String =
    """
      |students
      | id: integer, pk  student_name: text
      | 1                John
      | 2                Debbie
      |
      |class
      | id: integer, pk  name: text
      | 1                English
      | 2                Maths
      | 3                Spanish
      | 4                Biology
      | 5                Science
      | 6                Programming
      | 7                Law
      | 8                Commerce
      | 9                Physical Education
      |
      |student_class
      | studentid: integer, fk, students, id  classid: integer, fk, class, id  year: integer  semester: text  grade: text
      | 1                                     3                                2019           fall            B+
      | 1                                     5                                2018           winter          A
      | 1                                     9                                2019           summer          F
      | 2                                     1                                2018           fall            A+
      | 2                                     4                                2019           winter          B-
      | 2                                     5                                2018           summer          A-
      | 2                                     9                                2019           fall            B+
      |""".stripMargin

  db.create()
  insert(data)

}
