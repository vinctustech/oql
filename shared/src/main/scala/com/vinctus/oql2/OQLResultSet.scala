package com.vinctus.oql2

abstract class OQLResultSet {

  def peer: Any

  def next: Boolean

  def get(idx: Int): Any

}
