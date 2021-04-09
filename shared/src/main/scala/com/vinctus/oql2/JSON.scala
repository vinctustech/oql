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

    def aggregate[T](open: Char, seq: collection.Seq[T], close: Char)(render: T => Unit): Unit = {
      buf += open
      indent()

      val it = seq.iterator

      if (it.nonEmpty)
        render(it.next())

      while (it.hasNext) {
        buf += ','
        ln()
        margin()
        render(it.next())
      }

      dedent()
      buf += close
    }

    def jsonValue(value: Any): Unit =
      value match {
        case _: Double | _: Int | _: Boolean | null => buf ++= String.valueOf(value)
        case m: collection.Map[_, _]                => jsonObject(m.toSeq.asInstanceOf[Seq[(String, Any)]])
        case s: collection.Seq[_] if s.isEmpty      => buf ++= "[]"
        case s: collection.Seq[_]                   => aggregate('[', s, ']')(jsonValue)
        case a: Array[_]                            => jsonValue(a.toList)
        case p: Product                             => jsonObject(p.productElementNames zip p.productIterator toList)
        case _: String | _: Instant =>
          buf += '"'
          buf ++=
            List("\\" -> "\\\\", "\"" -> "\\\"", "\t" -> "\\t", "\n" -> "\\n", "\r" -> "\\r").foldLeft(value.toString) {
              case (acc, (c, r)) => acc.replace(c, r)
            }
          buf += '"'
      }

    def jsonObject(pairs: Seq[(String, Any)]): Unit =
      if (pairs.isEmpty)
        buf ++= "{}"
      else
        aggregate('{', pairs, '}') {
          case (k, v) =>
            jsonValue(k)
            buf ++= (if (format) ": " else ":")
            jsonValue(v)
        }

    jsonValue(value)
    buf.toString
  }

}
