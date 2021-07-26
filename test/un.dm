entity country {
 *id: bigint
  name: text
  rep: <rep>
}

entity rep {
 *id: bigint
  name: text
  country: country
}
