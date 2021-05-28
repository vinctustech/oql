//package com.vinctus.oql2
//
//import org.scalatest.freespec.AnyFreeSpec
//import org.scalatest.matchers.should.Matchers
//
//class StarTrekDBTests extends AnyFreeSpec with Matchers with StarTrekDB {
//
//  "query" in {
//    testmap("character { name species { origin { name } } } [species.name = 'Betazoid'] <name>") shouldBe
//      """
//        |List(
//        |  Map(
//        |    "name" -> "Deanna Troi",
//        |    "species" -> Map(
//        |      "origin" -> Map(
//        |        "name" -> "Betazed"
//        |      )
//        |    )
//        |  ),
//        |  Map(
//        |    "name" -> "Lwaxana Troi",
//        |    "species" -> Map(
//        |      "origin" -> Map(
//        |        "name" -> "Betazed"
//        |      )
//        |    )
//        |  )
//        |)""".trim.stripMargin
//  }
//
//  "ordered" in {
//    test("character {* home species {* origin}} <name>") shouldBe
//      """
//        |[
//        |  {
//        |    "char_id": 3,
//        |    "name": "Deanna Troi",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 3,
//        |      "name": "Betazoid",
//        |      "lifespan": 120,
//        |      "origin": {
//        |        "plan_id": 3,
//        |        "name": "Betazed",
//        |        "climate": "awesome weather"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 1,
//        |    "name": "James Tiberius Kirk",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 1,
//        |      "name": "Human",
//        |      "lifespan": 71,
//        |      "origin": {
//        |        "plan_id": 1,
//        |        "name": "Earth",
//        |        "climate": "not too bad"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 5,
//        |    "name": "Kurn, Son of Mogh",
//        |    "home": {
//        |      "plan_id": 4,
//        |      "name": "Qo'noS",
//        |      "climate": "turbulent"
//        |    },
//        |    "species": {
//        |      "spec_id": 4,
//        |      "name": "Klingon",
//        |      "lifespan": 150,
//        |      "origin": {
//        |        "plan_id": 4,
//        |        "name": "Qo'noS",
//        |        "climate": "turbulent"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 6,
//        |    "name": "Lwaxana Troi",
//        |    "home": {
//        |      "plan_id": 3,
//        |      "name": "Betazed",
//        |      "climate": "awesome weather"
//        |    },
//        |    "species": {
//        |      "spec_id": 3,
//        |      "name": "Betazoid",
//        |      "lifespan": 120,
//        |      "origin": {
//        |        "plan_id": 3,
//        |        "name": "Betazed",
//        |        "climate": "awesome weather"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 7,
//        |    "name": "Natasha Yar",
//        |    "home": {
//        |      "plan_id": 5,
//        |      "name": "Turkana IV",
//        |      "climate": null
//        |    },
//        |    "species": {
//        |      "spec_id": 1,
//        |      "name": "Human",
//        |      "lifespan": 71,
//        |      "origin": {
//        |        "plan_id": 1,
//        |        "name": "Earth",
//        |        "climate": "not too bad"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 2,
//        |    "name": "Spock",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 2,
//        |      "name": "Vulcan",
//        |      "lifespan": 220,
//        |      "origin": {
//        |        "plan_id": 2,
//        |        "name": "Vulcan",
//        |        "climate": "pretty hot"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 4,
//        |    "name": "Worf, Son of Mogh",
//        |    "home": null,
//        |    "species": {
//        |      "spec_id": 4,
//        |      "name": "Klingon",
//        |      "lifespan": 150,
//        |      "origin": {
//        |        "plan_id": 4,
//        |        "name": "Qo'noS",
//        |        "climate": "turbulent"
//        |      }
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "deep many-to-one selection" in {
//    test("character {* home species {* origin}} [species.origin.name = 'Vulcan']") shouldBe
//      """
//        |[
//        |  {
//        |    "char_id": 2,
//        |    "name": "Spock",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 2,
//        |      "name": "Vulcan",
//        |      "lifespan": 220,
//        |      "origin": {
//        |        "plan_id": 2,
//        |        "name": "Vulcan",
//        |        "climate": "pretty hot"
//        |      }
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "ordered resource selection" in {
//    test("character {* home species {* origin}} [char_id < 4] <name>") shouldBe
//      """
//        |[
//        |  {
//        |    "char_id": 3,
//        |    "name": "Deanna Troi",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 3,
//        |      "name": "Betazoid",
//        |      "lifespan": 120,
//        |      "origin": {
//        |        "plan_id": 3,
//        |        "name": "Betazed",
//        |        "climate": "awesome weather"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 1,
//        |    "name": "James Tiberius Kirk",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 1,
//        |      "name": "Human",
//        |      "lifespan": 71,
//        |      "origin": {
//        |        "plan_id": 1,
//        |        "name": "Earth",
//        |        "climate": "not too bad"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 2,
//        |    "name": "Spock",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 2,
//        |      "name": "Vulcan",
//        |      "lifespan": 220,
//        |      "origin": {
//        |        "plan_id": 2,
//        |        "name": "Vulcan",
//        |        "climate": "pretty hot"
//        |      }
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "query builder deep many-to-one selection" in {
//    db.queryBuilder()
//      .query("character { * home species { * origin } <name> }")
//      .select("species.origin.name = 'Vulcan'")
//      .json() shouldBe
//      """
//        |[
//        |  {
//        |    "char_id": 2,
//        |    "name": "Spock",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 2,
//        |      "name": "Vulcan",
//        |      "lifespan": 220,
//        |      "origin": {
//        |        "plan_id": 2,
//        |        "name": "Vulcan",
//        |        "climate": "pretty hot"
//        |      }
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//  "query builder ordered resource selection" in {
//    db.queryBuilder()
//      .query("character { * home species { * origin } }")
//      .select("char_id < 4")
//      .order("name", "ASC")
//      .json() shouldBe
//      """
//        |[
//        |  {
//        |    "char_id": 3,
//        |    "name": "Deanna Troi",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 3,
//        |      "name": "Betazoid",
//        |      "lifespan": 120,
//        |      "origin": {
//        |        "plan_id": 3,
//        |        "name": "Betazed",
//        |        "climate": "awesome weather"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 1,
//        |    "name": "James Tiberius Kirk",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 1,
//        |      "name": "Human",
//        |      "lifespan": 71,
//        |      "origin": {
//        |        "plan_id": 1,
//        |        "name": "Earth",
//        |        "climate": "not too bad"
//        |      }
//        |    }
//        |  },
//        |  {
//        |    "char_id": 2,
//        |    "name": "Spock",
//        |    "home": {
//        |      "plan_id": 1,
//        |      "name": "Earth",
//        |      "climate": "not too bad"
//        |    },
//        |    "species": {
//        |      "spec_id": 2,
//        |      "name": "Vulcan",
//        |      "lifespan": 220,
//        |      "origin": {
//        |        "plan_id": 2,
//        |        "name": "Vulcan",
//        |        "climate": "pretty hot"
//        |      }
//        |    }
//        |  }
//        |]
//        |""".trim.stripMargin
//  }
//
//}
