package org.laolittle.plugin

internal const val stopSymbol = """，,。.！!？?：:“”"/\s"""

internal val usefulRegex = Regex("[$stopSymbol]+")