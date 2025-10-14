package exp.compiler.klox.common

import exp.compiler.klox.lang.Expr
import exp.compiler.klox.fronted.Parser
import exp.compiler.klox.fronted.Scanner
import exp.compiler.klox.lang.Token

// std
internal fun eprintln(message: Any?) {
    System.err.println(message)
}

// Lox.kt
internal fun String.scanTokens(): List<Token> = Scanner(this).scanTokens()
internal fun List<Token>.parse(): Expr? = Parser(this).parse()

// Scanner.kt
internal fun Char.isIdentifierStart() = isLetter() || this == '_'
internal fun Char.isIdentifierPart() = isIdentifierStart() || isDigit()
