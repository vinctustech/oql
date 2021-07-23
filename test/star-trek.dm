entity planet {
 *plan_id: integer
  name: text
  climate: text
}

entity species {
 *spec_id: integer
  name: text
  lifespan: integer
  origin: planet
}

entity character {
 *char_id: integer
  name: text
  home: planet
  species: species
}
