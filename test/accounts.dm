entity account {
 *id: int
  name: text!
  stores: [store]
  users: [user]
}

entity store {
 *id: int
  account: account!
  name: text!
  users: [user] (users_stores)
  vehicles: [vehicle]
  trips: [trip]
}

entity user {
 *id: int
  account: account!
  email: text!
  password: text!
  name: text!
  stores: [store] (users_stores)
  vehicle: <vehicle>.driver
}

entity users_stores {
  user: user
  store: store
}

entity vehicle {
 *id: int
  driver: user
  make: text!
  store: store!
  trips: [trip]
}

entity customer {
 *id: int
  store: store!
  name: text
}

entity trip {
 *id: int
  state: text!
  customer: customer
  store: store!
  vehicle: vehicle
}
