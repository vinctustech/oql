package com.vinctus.oql2

import scala.collection.mutable

class Parameters(map: collection.Map[String, Any]) {

  private val requested = new mutable.HashSet[String]

  def keySet: Set[String] = requested.toSet

  def get(key: String): Option[Any] = {
    requested += key
    map get key
  }

}
