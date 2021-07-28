package com.vinctus.oql

abstract class Conversions {

  def timestamp(t: String): Any

  def uuid(id: String): Any

  def bigint(n: String): Any

  def decimal(n: String, precision: Int, scale: Int): Any

}
