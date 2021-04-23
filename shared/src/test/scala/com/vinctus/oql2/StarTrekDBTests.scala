package com.vinctus.oql2

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class StarTrekDBTests extends AnyFreeSpec with Matchers with StarTrekDB {

  "query" in {
    testmap("character { name species { origin { name } } } [species.name = 'Betazoid'] <name>") shouldBe
      List(
        Map("name" -> "Deanna Troi", "species" -> Map("origin" -> Map("name" -> "Betazed"))),
        Map("name" -> "Lwaxana Troi", "species" -> Map("origin" -> Map("name" -> "Betazed")))
      )
  }

}
