package com.vinctus.oql2

abstract class DataSource(val name: String) {

  def connect: Connection

}
