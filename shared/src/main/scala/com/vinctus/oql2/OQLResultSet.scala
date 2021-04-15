package com.vinctus.oql2

abstract class OQLResultSet {

  def peer: Any

  def next: Boolean

  /**
    * Gets the value of the column at zero-based index '`idx`' in the current row of this result set.
    *
    * @param idx zero-based column index
    * @return value of designated column
    */
  def get(idx: Int): Any

  def getString(idx: Int): String

}
