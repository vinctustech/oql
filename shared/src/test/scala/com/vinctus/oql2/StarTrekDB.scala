package com.vinctus.oql2

trait StarTrekDB extends Test {

  val dm: String =
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
  val db = new AbstractOQL(dm, ds)
  val data: String =
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

  db.create()
  insert(data)

}
