entity country {
 *id: integer
  name: text
  rep: <rep>
}

entity rep {
 *id: integer
  name: text
  country: country
}
