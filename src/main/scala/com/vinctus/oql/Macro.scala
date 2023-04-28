package com.vinctus.oql

case class Macro(definition: String, parameters: Seq[String])

val macroSubstitutionRegex = """\$\$|\$([A-Za-z_][A-Za-z0-9_]*)""".r
