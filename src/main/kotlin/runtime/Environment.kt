package exp.compiler.klox.runtime

import exp.compiler.klox.common.RuntimeError
import exp.compiler.klox.lang.Token

class Environment (
    private val enclosing : Environment? = null
){
    private var values = mutableMapOf<String, Any?>()

    internal fun define(name: String, value: Any?) {
        values += name to value
    }

    internal fun get(name: Token): Any? = when {
        values.contains(name.lexeme) -> values[name.lexeme]
        enclosing != null -> enclosing.get(name)
        else -> throw RuntimeError(name.line, "Undefined variable '${name.lexeme}'.")
    }

    internal fun assign(name: Token, value: Any?) = when {
        values.contains(name.lexeme) -> values[name.lexeme] = value
        else -> throw RuntimeError(name.line, "Undefined variable '${name.lexeme}'.")
    }
}
