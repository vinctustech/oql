package com.vinctus.oql

object Testing {

  val unDB =
    new RDBConnection("""
        |country
        | id: integer, pk          name: text
        | 1                Nigeria
        | 2                Ghana
        | 3                South Africa
        | 4                Republic of China (Taiwan)
        |
        |rep
        | id: integer, pk    name: text    country: integer, fk, country, id
        | 1                Abubakar Ahmad  1
        | 2                Joseph Nkrumah  2
        | 3                Lauren Zuma     3
        | 4                Batman          null
        |""".stripMargin)
  val unER =
    new OQL(
      unDB,
      """
        |entity country {
        | *id: integer
        |  name: text
        |  rep: <rep>
        |}
        |
        |entity rep {
        | *id: integer
        |  name: text
        |  country: country
        |}
        |""".stripMargin
    )

  val employeesDB =
    new RDBConnection(
      """
        |department
        | dep_id: integer, pk  dep_name: text  dep_location: text 
        | 1001                 FINANCE         SYDNEY             
        | 2001                 AUDIT           MELBOURNE          
        | 3001                 MARKETING       PERTH              
        | 4001                 PRODUCTION      BRISBANE           
        |
        |employee
        | emp_id: integer, pk  emp_name: text  job_title: text  manager_id: integer, fk, employee, emp_id  hire_date: date  salary: decimal  commission: decimal  dep_id: integer, fk, department, dep_id
        | 68319                KAYLING         PRESIDENT        null                                       1991-11-18       6000.00           null                  1001
        | 66928                BLAZE           MANAGER          68319                                      1991-05-01       2750.00           null                  3001
        | 67832                CLARE           MANAGER          68319                                      1991-06-09       2550.00           null                  1001
        | 65646                JONAS           MANAGER          68319                                      1991-04-02       2957.00           null                  2001
        | 67858                SCARLET         ANALYST          65646                                      1997-04-19       3100.00           null                  2001
        | 69062                FRANK           ANALYST          65646                                      1991-12-03       3100.00           null                  2001
        | 63679                SANDRINE        CLERK            69062                                      1990-12-18       900.00            null                  2001
        | 64989                ADELYN          SALESREP         66928                                      1991-02-20       1700.00           400.00                3001
        | 65271                WADE            SALESREP         66928                                      1991-02-22       1350.00           600.00                3001
        | 66564                MADDEN          SALESREP         66928                                      1991-09-28       1350.00           1500.00               3001
        | 68454                TUCKER          SALESREP         66928                                      1991-09-08       1600.00           0.00                  3001
        | 68736                ADNRES          CLERK            67858                                      1997-05-23       1200.00           null                  2001
        | 69000                JULIUS          CLERK            66928                                      1991-12-03       1050.00           null                  3001
        | 69324                MARKER          CLERK            67832                                      1992-01-23       1400.00           null                  1001
        |
        |salary_grade
        | grade: integer, pk  min_sal: decimal  max_sal: decimal
        | 1                   800                1300              
        | 2                   1301               1500              
        | 3                   1501               2100              
        | 4                   2101               3100              
        | 5                   3101               9999              
        |""".stripMargin)
  val employeesER =
    new OQL(
      employeesDB,
      """
        |entity employee {
        | *emp_id: integer
        |  name (emp_name): text
        |  job_title: text
        |  manager (manager_id): employee
        |  hire_date: date
        |  salary: decimal
        |  commission: decimal
        |  department (dep_id): department
        |}
        |
        |entity department {
        | *dep_id: integer
        |  name (dep_name): text
        |  dep_location: text
        |}
        |
        |entity salary_grade {
        | *grade: integer
        |  min_sal: decimal
        |  max_sal: decimal
        |}
        |""".stripMargin
    )

  val ordersDB =
    new RDBConnection(
      """
        |agent
        | agent_code: text, pk  agent_name: text  working_area: text  commission: decimal  phone_no: text
        | A007                  Ramasundar        Bangalore           0.15                  077-25814763
        | A003                  Alex              London              0.13                  075-12458969
        | A008                  Alford            New York            0.12                  044-25874365
        | A011                  Ravi Kumar        Bangalore           0.15                  077-45625874
        | A010                  Santakumar        Chennai             0.14                  007-22388644
        | A012                  Lucida            San Jose            0.12                  044-52981425
        | A005                  Anderson          Brisban             0.13                  045-21447739
        | A001                  Subbarao          Bangalore           0.14                  077-12346674
        | A002                  Mukesh            Mumbai              0.11                  029-12358964
        | A006                  McDen             London              0.15                  078-22255588
        | A004                  Ivan              Torento             0.15                  008-22544166
        | A009                  Benjamin          Hampshair           0.11                  008-22536178
        |
        |customer
        | cust_code: text, pk  name: text
        | C00002               C00002 asdf
        | C00003               C00003 asdf
        | C00023               C00023 asdf
        | C00007               C00007 asdf
        | C00008               C00008 asdf
        | C00025               C00025 asdf
        | C00004               C00004 asdf
        | C00008               C00008 asdf
        | C00021               C00021 asdf
        | C00025               C00025 asdf
        | C00011               C00011 asdf
        | C00001               C00001 asdf
        | C00020               C00020 asdf
        | C00006               C00006 asdf
        | C00005               C00005 asdf
        | C00018               C00018 asdf
        | C00014               C00014 asdf
        | C00022               C00022 asdf
        | C00009               C00009 asdf
        | C00010               C00010 asdf
        | C00017               C00017 asdf
        | C00022               C00022 asdf
        | C00024               C00024 asdf
        | C00015               C00015 asdf
        | C00009               C00009 asdf
        | C00007               C00007 asdf
        | C00012               C00012 asdf
        | C00009               C00009 asdf
        | C00015               C00015 asdf
        | C00019               C00019 asdf
        | C00007               C00007 asdf
        | C00016               C00016 asdf
        | C00022               C00022 asdf
        | C00012               C00012 asdf
        |
        |order
        | ord_num: integer, pk  ord_amount: decimal  advance_amount: decimal  ord_date: text  cust_code: text, fk, customer, cust_code  agent_code: text, fk, agent, agent_code
        | 200114                3500                  2000                      15-AUG-08       C00002                                    A008
        | 200122                2500                  400                       16-SEP-08       C00003                                    A004
        | 200118                500                   100                       20-JUL-08       C00023                                    A006
        | 200119                4000                  700                       16-SEP-08       C00007                                    A010
        | 200121                1500                  600                       23-SEP-08       C00008                                    A004
        | 200130                2500                  400                       30-JUL-08       C00025                                    A011
        | 200134                4200                  1800                      25-SEP-08       C00004                                    A005
        | 200108                4000                  600                       15-FEB-08       C00008                                    A004
        | 200103                1500                  700                       15-MAY-08       C00021                                    A005
        | 200105                2500                  500                       18-JUL-08       C00025                                    A011
        | 200109                3500                  800                       30-JUL-08       C00011                                    A010
        | 200101                3000                  1000                      15-JUL-08       C00001                                    A008
        | 200111                1000                  300                       10-JUL-08       C00020                                    A008
        | 200104                1500                  500                       13-MAR-08       C00006                                    A004
        | 200106                2500                  700                       20-APR-08       C00005                                    A002
        | 200125                2000                  600                       10-OCT-08       C00018                                    A005
        | 200117                800                   200                       20-OCT-08       C00014                                    A001
        | 200123                500                   100                       16-SEP-08       C00022                                    A002
        | 200120                500                   100                       20-JUL-08       C00009                                    A002
        | 200116                500                   100                       13-JUL-08       C00010                                    A009
        | 200124                500                   100                       20-JUN-08       C00017                                    A007
        | 200126                500                   100                       24-JUN-08       C00022                                    A002
        | 200129                2500                  500                       20-JUL-08       C00024                                    A006
        | 200127                2500                  400                       20-JUL-08       C00015                                    A003
        | 200128                3500                  1500                      20-JUL-08       C00009                                    A002
        | 200135                2000                  800                       16-SEP-08       C00007                                    A010
        | 200131                900                   150                       26-AUG-08       C00012                                    A012
        | 200133                1200                  400                       29-JUN-08       C00009                                    A002
        | 200100                1000                  600                       08-JAN-08       C00015                                    A003
        | 200110                3000                  500                       15-APR-08       C00019                                    A010
        | 200107                4500                  900                       30-AUG-08       C00007                                    A010
        | 200112                2000                  400                       30-MAY-08       C00016                                    A007
        | 200113                4000                  600                       10-JUN-08       C00022                                    A002
        | 200102                2000                  300                       25-MAY-08       C00012                                    A012
        |""".stripMargin
    )
  val ordersER =
    new OQL(
      ordersDB,
      """
        |entity agent {
        | *agent_code: text
        |  agent_name: text
        |  working_area: text
        |  commission: decimal
        |  phone_no: text
        |  orders: [order]
        |}
        |
        |entity order {
        | *ord_num: integer
        |  ord_amount: decimal
        |  advance_amount: decimal
        |  ord_date: text
        |  customer (cust_code): customer
        |  agent (agent_code): agent
        |}
        |
        |entity customer {
        | *cust_code: text
        |  name: text
        |}
        |""".stripMargin
    )

  val studentDB =
    new RDBConnection(
      """
        |students
        | id: integer, pk  stu_name: text
        | 1                John
        | 2                Debbie
        |
        |class
        | id: integer, pk      name: text
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
        | 1                                    3                                2019           fall            B+
        | 1                                    5                                2018           winter          A
        | 1                                    9                                2019           summer          F
        | 2                                    1                                2018           fall            A+
        | 2                                    4                                2019           winter          B-
        | 2                                    5                                2018           summer          A-
        | 2                                    9                                2019           fall            B+
        |""".stripMargin
    )
  val studentER =
    new OQL(
      studentDB,
      """
        |entity class {
        | *id: integer
        |  name: text
        |  students: [student] (enrollment)
        |}
        |
        |entity student (students) {
        | *id: integer
        |  name (stu_name): text
        |  classes: [class] (enrollment)
        |}
        |
        |entity enrollment (student_class) {
        |  student (studentid): student
        |  class (classid): class
        |  year: integer
        |  semester: text
        |  grade: text
        |}
        |""".stripMargin
    )

  val starTrekDB =
    new RDBConnection(
      """
        |planet
        | plan_id: integer, pk  name: text   climate: text  
        | 1                     Earth       not too bad     
        | 2                     Vulcan      pretty hot      
        | 3                     Betazed     awesome weather 
        | 4                     Qo'noS      turbulent       
        | 5                     Turkana IV  null            
        |
        |species
        | spec_id: integer, pk  name: text  lifespan: integer  origin: integer, fk, planet, plan_id 
        | 1                     Human       71                 1                                    
        | 2                     Vulcan      220                2                                    
        | 3                     Betazoid    120                3                                    
        | 4                     Klingon     150                4                                    
        |
        |character
        | char_id: integer, pk      name: text       home: integer, fk, planet, plan_id  species: integer, fk, species, spec_id 
        | 1                     James Tiberius Kirk  1                                   1                                      
        | 2                     Spock                1                                   2                                      
        | 3                     Deanna Troi          1                                   3                                      
        | 4                     Worf, Son of Mogh    null                                4
        | 5                     Kurn, Son of Mogh    4                                   4                                      
        | 6                     Lwaxana Troi         3                                   3                                      
        | 7                     Natasha Yar          5                                   1
        |""".stripMargin
    )
  val starTrekER =
    new OQL(
      starTrekDB,
      """
      |entity planet {
      | *plan_id: integer
      |  name: text
      |  climate: text
      |}
      |
      |entity species {
      | *spec_id: integer
      |  name: text
      |  lifespan: integer
      |  origin: planet
      |}
      |
      |entity character {
      | *char_id: integer
      |  name: text
      |  home: planet
      |  species: species
      |}
      |""".stripMargin
    )

}
