package com.vinctus.oql

import com.vinctus.sjs_utils.fromJS
import java.time.Instant
import scala.scalajs.js

object ScalaConversions extends Conversions {

  def timestamp(t: String): Any = Instant.parse(if (t endsWith "Z") t else t :+ 'Z')

  def uuid(id: String): Any = id

  def bigint(n: String): Any = n.toLong

  def decimal(n: String, precision: Int, scale: Int): Any = BigDecimal(n).setScale(scale)

  def jsonBinary(v: js.Any): Any = fromJS(v)

  def jsonString(v: js.Any): Any = sys.error("NOT DONE YET")

}
