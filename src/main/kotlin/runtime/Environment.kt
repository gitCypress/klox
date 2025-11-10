package exp.compiler.klox.runtime

import exp.compiler.klox.common.RuntimeError
import exp.compiler.klox.lang.Token

class Environment(
    private val enclosing: Environment? = null
) {
    private var values = mutableMapOf<String, Any?>()

    internal fun define(name: String, value: Any?) {
        values += name to value
    }

    internal operator fun get(name: Token): Any? = when {
        values.contains(name.lexeme) -> values[name.lexeme]
        enclosing != null -> enclosing[name]
        else -> throw RuntimeError(name.line, "Undefined variable '${name.lexeme}'.")
    }

    internal fun getAt(distance: Int, name: String): Any? = ancestor(distance)?.values[name]

    private fun ancestor(distance: Int): Environment? =
        generateSequence(this) { it.enclosing }
            .drop(distance)
            .firstOrNull()

    internal fun assign(name: Token, value: Any?): Unit = when {
        values.contains(name.lexeme) -> values[name.lexeme] = value
        enclosing != null -> enclosing.assign(name, value)
        else -> throw RuntimeError(name.line, "Undefined variable '${name.lexeme}'.")
    }

    internal fun assignAt(distance: Int, name: Token, value: Any?) = ancestor(distance)
        ?.values
        ?.put(name.lexeme, value)
}