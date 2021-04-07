package com.vinctus.oql2

trait OQLDataSource {

  val name: String

  def connect: OQLConnection

  def create(model: DataModel): Unit

}
