entity employee {
 *id: bigint
  firstName: text
  lastName: text
  manager: employee
  job: job
  department: department
}

entity job {
 *id: bigint
  jobTitle: text
  employees: [employee]
  departments: [department] (employee)
}

entity department {
 *id: bigint
  departmentName: text
  employees: [employee]
  jobs: [job] (employee)
}
