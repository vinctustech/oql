package com.vinctus.oql2

trait MinimalEmployeeDB extends Test {

  val dm: String =
    """
      |entity employee {
      | *id: bigint
      |  firstName: text
      |  lastName: text
      |  manager: employee
      |}
      |""".stripMargin
  val db = new OQL(dm, new H2_mem)

  db.create()
  db.execute(_.insert("""insert into employee (id, firstName, lastName, manager) values 
                        |(100,'Steven','King',null),
                        |(101,'Neena','Kochhar',100),
                        |(102,'Lex','De Haan',100),
                        |(103,'Alexander','Hunold',102),
                        |(104,'Bruce','Ernst',103)
                        |""".stripMargin))

}
