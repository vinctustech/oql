package com.vinctus.oql2

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

object JSON {

  private val EOI = '\uE000'
  private val HEX = {
    val a = new Array[Int](128)

    List(
      '0' -> 0,
      '1' -> 1,
      '2' -> 2,
      '3' -> 3,
      '4' -> 4,
      '5' -> 5,
      '6' -> 6,
      '7' -> 7,
      '8' -> 8,
      '9' -> 9,
      'a' -> 10,
      'A' -> 10,
      'b' -> 11,
      'B' -> 11,
      'c' -> 12,
      'C' -> 12,
      'd' -> 13,
      'D' -> 13,
      'e' -> 14,
      'E' -> 14,
      'f' -> 15,
      'F' -> 15
    ) foreach { case (k, v) => a(k) = v }

    a to ArraySeq
  }

  private def hex(c: Char) = if (c < 128) HEX(c) else 0

  def readArray(json: String): IndexedSeq[Any] = {
    var idx: Int = 0

    def next: Char =
      if (idx > json.length) sys.error("past end of JSON string")
      else if (idx == json.length) EOI
      else json.charAt(idx)

    def ch: Char = {
      val c = next

      advance()
      c
    }

    def advance(): Unit = idx += 1

    def prev: Char = json.charAt(idx - 1)

    def space(): Unit = while (next.isWhitespace) advance()

    def chmatch(c: Char): Unit =
      if (ch != c) error(if (c == EOI) "expected end of input" else s"expected '$c', but found '$prev':\n$json\n${(" " * idx) :+ '^'}")

    def delim(c: Char): Unit = {
      chmatch(c)
      space()
    }

    def readArray: IndexedSeq[Any] = {
      val buf = new ArrayBuffer[Any]

      delim('[')

      @tailrec
      def elem(): Unit = {
        buf += readValue

        if (next == ',') {
          advance()
          space()
          elem()
        }
      }

      if (next != ']')
        elem()

      delim(']')
      buf to ArraySeq
    }

    def error(str: String) = sys.error(str)

    def readValue: Any =
      next match {
        case `EOI`                      => error("unexpected end of JSON string")
        case '['                        => readArray
        case '"'                        => readString
        case d if d.isDigit || d == '-' => readNumber
        case 'n'                        => value("null", null)
        case 't'                        => value("true", true)
        case 'f'                        => value("false", false)
      }

    def readString: String = {
      val buf = new StringBuilder

      chmatch('"')

      @tailrec
      def content(): Unit =
        ch match {
          case '\\' =>
            buf +=
              (ch match {
                case '\\' => '\\'
                case '"'  => '"'
                case '/'  => '/'
                case 'b'  => '\b'
                case 'f'  => '\f'
                case 'n'  => '\n'
                case 'r'  => '\r'
                case 't'  => '\t'
                case 'u'  => (hex(ch) << 12 | hex(ch) << 8 | hex(ch) << 4 | hex(ch)).toChar
              })
            content()
          case '"' =>
          case c =>
            buf += c
            content()
        }

      content()
      space()

      buf.toString
    }

    def readNumber: String = {
      val buf = new StringBuilder
      var c: Char = next

      while (c.isDigit || c == '.' || c == '-' || c == 'e' || c == 'E') {
        buf += c
        advance()
        c = next
      }

      space()
      buf.toString
    }

    def value(s: String, v: Any): Any = {
      for (i <- 0 until s.length) {
        if (next == EOI) error(s"unexpected end of JSON string: trying to match '$s'")
        else if (ch != s.charAt(i)) error(s"mismatch")
      }

      space()
      v
    }

    space()

    val v = readArray

    chmatch(EOI)
    v
  }

  def apply(value: Any, platformSpecific: PartialFunction[Any, String], tab: Int = 2, format: Boolean = false): String = {
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
        case p if platformSpecific isDefinedAt p => platformSpecific(p)
        case _: Double | _: Int | _: Long | _: Boolean | _: BigDecimal | _: java.math.BigDecimal | null =>
          buf ++= String.valueOf(value)
        case m: collection.Map[_, _]           => jsonObject(m.toSeq.asInstanceOf[Seq[(String, Any)]])
        case s: collection.Seq[_] if s.isEmpty => buf ++= "[]"
        case s: collection.Seq[_]              => aggregate('[', s, ']')(jsonValue)
        case a: Array[_]                       => jsonValue(a.toList)
        case p: Product                        => jsonObject(p.productElementNames zip p.productIterator toList)
//        case t: Timestamp                      => jsonValue(t.toInstant.toString) //  | _: Instant  _: java.util.UUID
        case _: String =>
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
    ln()
    buf.toString
  }

}
