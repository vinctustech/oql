package com.vinctus.oql

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import typings.pg.mod.types
import typings.pgTypes.mod.TypeId

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class StarTrekDBTests extends AsyncFreeSpec with Matchers {

  g.require("source-map-support").install()
  types.setTypeParser(114.asInstanceOf[TypeId], (s: String) => s) // tell node-pg not to parse JSON
  types.setTypeParser(1114.asInstanceOf[TypeId], (s: String) => new js.Date(s"$s+00:00"))

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db =
    new OQL_NodePG(g.require("fs").readFileSync("test/star-trek.dm").toString, "localhost", 5432, "postgres", "postgres", "docker", false, 1000, 5)

  def test(oql: String, parameters: (String, Any)*): Future[String] = db.json(oql, parameters: _*)

  "query" in {
    test("character { name species { origin { name } } } [species.name = 'Betazoid'] <name>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "name": "Deanna Troi",
          |    "species": {
          |      "origin": {
          |        "name": "Betazed"
          |      }
          |    }
          |  },
          |  {
          |    "name": "Lwaxana Troi",
          |    "species": {
          |      "origin": {
          |        "name": "Betazed"
          |      }
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "ordered" in {
    test("character {* home species {* origin}} <name>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "char_id": 3,
          |    "name": "Deanna Troi",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 3,
          |      "name": "Betazoid",
          |      "lifespan": 120,
          |      "origin": {
          |        "plan_id": 3,
          |        "name": "Betazed",
          |        "climate": "awesome weather"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 1,
          |    "name": "James Tiberius Kirk",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 1,
          |      "name": "Human",
          |      "lifespan": 71,
          |      "origin": {
          |        "plan_id": 1,
          |        "name": "Earth",
          |        "climate": "not too bad"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 5,
          |    "name": "Kurn, Son of Mogh",
          |    "home": {
          |      "plan_id": 4,
          |      "name": "Qo'noS",
          |      "climate": "turbulent"
          |    },
          |    "species": {
          |      "spec_id": 4,
          |      "name": "Klingon",
          |      "lifespan": 150,
          |      "origin": {
          |        "plan_id": 4,
          |        "name": "Qo'noS",
          |        "climate": "turbulent"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 6,
          |    "name": "Lwaxana Troi",
          |    "home": {
          |      "plan_id": 3,
          |      "name": "Betazed",
          |      "climate": "awesome weather"
          |    },
          |    "species": {
          |      "spec_id": 3,
          |      "name": "Betazoid",
          |      "lifespan": 120,
          |      "origin": {
          |        "plan_id": 3,
          |        "name": "Betazed",
          |        "climate": "awesome weather"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 7,
          |    "name": "Natasha Yar",
          |    "home": {
          |      "plan_id": 5,
          |      "name": "Turkana IV",
          |      "climate": null
          |    },
          |    "species": {
          |      "spec_id": 1,
          |      "name": "Human",
          |      "lifespan": 71,
          |      "origin": {
          |        "plan_id": 1,
          |        "name": "Earth",
          |        "climate": "not too bad"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 2,
          |    "name": "Spock",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 2,
          |      "name": "Vulcan",
          |      "lifespan": 220,
          |      "origin": {
          |        "plan_id": 2,
          |        "name": "Vulcan",
          |        "climate": "pretty hot"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 4,
          |    "name": "Worf, Son of Mogh",
          |    "home": null,
          |    "species": {
          |      "spec_id": 4,
          |      "name": "Klingon",
          |      "lifespan": 150,
          |      "origin": {
          |        "plan_id": 4,
          |        "name": "Qo'noS",
          |        "climate": "turbulent"
          |      }
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "deep many-to-one selection" in {
    test("character {* home species {* origin}} [species.origin.name = 'Vulcan']") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "char_id": 2,
          |    "name": "Spock",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 2,
          |      "name": "Vulcan",
          |      "lifespan": 220,
          |      "origin": {
          |        "plan_id": 2,
          |        "name": "Vulcan",
          |        "climate": "pretty hot"
          |      }
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "ordered resource selection" in {
    test("character {* home species {* origin}} [char_id < 4] <name>") map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "char_id": 3,
          |    "name": "Deanna Troi",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 3,
          |      "name": "Betazoid",
          |      "lifespan": 120,
          |      "origin": {
          |        "plan_id": 3,
          |        "name": "Betazed",
          |        "climate": "awesome weather"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 1,
          |    "name": "James Tiberius Kirk",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 1,
          |      "name": "Human",
          |      "lifespan": 71,
          |      "origin": {
          |        "plan_id": 1,
          |        "name": "Earth",
          |        "climate": "not too bad"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 2,
          |    "name": "Spock",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 2,
          |      "name": "Vulcan",
          |      "lifespan": 220,
          |      "origin": {
          |        "plan_id": 2,
          |        "name": "Vulcan",
          |        "climate": "pretty hot"
          |      }
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "query builder deep many-to-one selection" in {
    db.queryBuilder()
      .query("character { * home species { * origin } <name> }")
      .select("species.origin.name = 'Vulcan'")
      .json map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "char_id": 2,
          |    "name": "Spock",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 2,
          |      "name": "Vulcan",
          |      "lifespan": 220,
          |      "origin": {
          |        "plan_id": 2,
          |        "name": "Vulcan",
          |        "climate": "pretty hot"
          |      }
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

  "query builder ordered resource selection" in {
    db.queryBuilder()
      .query("character { * home species { * origin } }")
      .select("char_id < 4")
      .order("name", "ASC")
      .json map { result =>
      result shouldBe
        """
          |[
          |  {
          |    "char_id": 3,
          |    "name": "Deanna Troi",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 3,
          |      "name": "Betazoid",
          |      "lifespan": 120,
          |      "origin": {
          |        "plan_id": 3,
          |        "name": "Betazed",
          |        "climate": "awesome weather"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 1,
          |    "name": "James Tiberius Kirk",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 1,
          |      "name": "Human",
          |      "lifespan": 71,
          |      "origin": {
          |        "plan_id": 1,
          |        "name": "Earth",
          |        "climate": "not too bad"
          |      }
          |    }
          |  },
          |  {
          |    "char_id": 2,
          |    "name": "Spock",
          |    "home": {
          |      "plan_id": 1,
          |      "name": "Earth",
          |      "climate": "not too bad"
          |    },
          |    "species": {
          |      "spec_id": 2,
          |      "name": "Vulcan",
          |      "lifespan": 220,
          |      "origin": {
          |        "plan_id": 2,
          |        "name": "Vulcan",
          |        "climate": "pretty hot"
          |      }
          |    }
          |  }
          |]
          |""".trim.stripMargin
    }
  }

}
