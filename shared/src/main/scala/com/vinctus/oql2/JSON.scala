package com.vinctus.oql2

import java.time.Instant
import scala.language.postfixOps

object JSON {

  def apply(value: Any, tab: Int = 2, format: Boolean = false): String = {
    val buf = new StringBuilder
    var level = 0

    def ln(): Unit =
      if (format)
        buf += '\n'

    def indent(): Unit = {
      ln()
      level += tab
      margin()
    }

    def dedent(): Unit = {
      ln()
      level -= tab
      margin()
    }

    def margin(): Unit =
      if (format)
        buf ++= " " * level

    def aggregate[T](seq: collection.Seq[T])(render: T => Unit): Unit = {
      val it = seq.iterator

      if (it.nonEmpty)
        render(it.next())

      while (it.hasNext) {
        buf += ','
        ln()
        margin()
        render(it.next())
      }
    }

    def jsonValue(value: Any): Unit =
      value match {
        case _: Double | _: Int | _: Boolean | null => buf ++= String.valueOf(value)
        case m: collection.Map[_, _]                => jsonObject(m.toSeq.asInstanceOf[Seq[(String, Any)]])
        case s: collection.Seq[_] if s.isEmpty      => buf ++= "[]"
        case s: collection.Seq[_] =>
          buf += '['
          indent()
          aggregate(s)(jsonValue)
          dedent()
          buf += ']'
        case a: Array[_] => jsonValue(a.toList)
        case p: Product  => jsonObject(p.productElementNames zip p.productIterator toList)
        case _: String | _: Instant =>
          buf += '"'
          buf ++= value.toString
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
          buf += '"'
      }

    def jsonObject(pairs: Seq[(String, Any)]): Unit = {
      if (pairs.isEmpty)
        buf ++= "{}"
      else {
        buf += '{'
        indent()
        aggregate(pairs) {
          case (k, v) =>
            jsonValue(k)
            buf ++= (if (format) ": " else ":")
            jsonValue(v)
        }
        dedent()
        buf += '}'
      }
    }

    jsonValue(value)
    buf.toString
  }

}
