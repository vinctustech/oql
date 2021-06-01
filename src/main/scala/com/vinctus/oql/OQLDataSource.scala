package com.vinctus.oql

trait OQLDataSource {

  val name: String

  def connect: OQLConnection

}
