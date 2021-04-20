package com.vinctus.oql2

trait EmployeeDBPG extends Test {

  val dm: String =
    """
      |entity employee {
      | *id: bigint
      |  firstName: text
      |  lastName: text
      |  manager: employee
      |  job: job
      |  department: department
      |}
      |
      |entity job {
      | *id: bigint
      |  jobTitle: text
      |  employees: [employee]
      |  departments: [department] (employee)
      |}
      |
      |entity department {
      | *id: bigint
      |  departmentName: text
      |  employees: [employee]
      |  jobs: [job] (employee)
      |}
      |""".stripMargin
  val db = new OQL(dm, new PG("localhost", 5432, "postgres", "postgres", "docker"))

//  db.create()
//  db.execute(_.insert("""insert into job (id, jobTitle) values
//                        |(4,'President'),
//                        |(5,'Administration Vice President'),
//                        |(9,'Programmer'),
//                        |(20,'IT Manager')
//                        |""".stripMargin))
//  db.execute(_.insert("""insert into department (id, departmentName) values
//                        |(9,'Executive'),
//                        |(6,'IT')
//                        |""".stripMargin))
//  db.execute(_.insert("""insert into employee (id, firstName, lastName, manager, job, department) values
//                        |(100,'Steven','King',null,4,9),
//                        |(101,'Neena','Kochhar',100,5,9),
//                        |(102,'Lex','De Haan',100,5,9),
//                        |(103,'Alexander','Hunold',102,20,6),
//                        |(104,'Bruce','Ernst',103,9,6)
//                        |""".stripMargin))

}
