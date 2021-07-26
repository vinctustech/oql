entity class {
 *id: integer
  name: text
  students: [student] (enrollment)
}

entity student (students) {
 *id: integer
  name (student_name): text
  classes: [class] (enrollment)
}

entity enrollment (student_class) {
  student (studentid): student!
  class (classid): class!
  year: integer
  semester: text
  grade: text
}
