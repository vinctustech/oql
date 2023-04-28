package com.vinctus.oql

import scala.collection.immutable

case class Macro(definition: String, parameters: immutable.ArraySeq[String])

val macroSubstitutionRegex = """\$\$|\$([A-Za-z_][A-Za-z0-9_]*)""".r
