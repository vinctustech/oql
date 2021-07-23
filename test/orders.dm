entity agent {
 *agent_code: text
  agent_name: text
  working_area: text
  commission: decimal(6, 2)
  phone_no: text
  orders: [order]
}

entity order {
 *ord_num: integer
  ord_amount: decimal(6, 2)
  advance_amount: decimal(6, 2)
  ord_date: text
  customer (cust_code): customer
  agent (agent_code): agent!
}

entity customer {
 *cust_code: text
  name: text
}
