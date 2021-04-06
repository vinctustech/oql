package com.vinctus.oql2

trait OQLDataSource {

  val name: String

  def connect: Connection

  def create(model: DataModel): Unit

}
