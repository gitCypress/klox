package exp.compiler.klox.common

// std
internal fun eprintln(message: Any?) {
    System.err.println(message)
}

internal fun tprintln(message: Any?) {
    if (LConfig.isDebug) println(message)
}

// Scanner.kt
internal fun Char.isIdentifierStart() = isLetter() || this == '_'
internal fun Char.isIdentifierPart() = isIdentifierStart() || isDigit()
