package exp.compiler.klox.common

import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType

internal class RuntimeError(val line: Int, message: String?) : RuntimeException(message)
internal class ParseError : RuntimeException()

internal object LErr {
    var hadError: Boolean = false
        private set
    var hadRuntimeError: Boolean = false
        private set

    fun error(line: Int, message: String) = report(line, "", message)

    fun error(token: Token, message: String) = when (token.type) {
        TokenType.EOF -> report(token.line, "EOF", message)
        else -> report(token.line, "at '${token.lexeme}'", message)
    }

    fun runtimeError(error: RuntimeError) {
        eprintln("${error.message}\n[line " + error.line + "]")
        hadRuntimeError = true
    }

    fun resetError() {
        hadError = false
    }

    private fun report(line: Int, where: String, message: String) {
        eprintln("[line $line] Error $where: $message")
        hadError = true
    }
}
