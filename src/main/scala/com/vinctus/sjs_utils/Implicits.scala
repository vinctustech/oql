package com.vinctus.sjs_utils

object Implicits {

  implicit def product2seq(p: Product): Seq[(String, Any)] = p.productElementNames zip p.productIterator toSeq

  implicit def product2map(p: Product): Map[String, Any] = product2seq(p).toMap

  implicit def dotString(f: DynamicMapField): String = f.asString

  implicit def dotInt(f: DynamicMapField): Int = f.asInt

  implicit def dotDynamicMap(f: DynamicMapField): DynamicMap = f.asMap

  implicit def dotMap(f: DynamicMapField): DynamicMap = dotDynamicMap(f)

}
