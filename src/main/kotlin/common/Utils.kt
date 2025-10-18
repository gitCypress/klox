package exp.compiler.klox.common

internal fun Any?.stringify(): String = when (this) {
    null -> "nil"
    is Double -> this.toString().removeSuffix(".0")
    else -> this.toString()
}