package exp.compiler.klox.runtime

import exp.compiler.klox.common.RuntimeError
import exp.compiler.klox.lang.Token

class Environment(
    private val enclosing: Environment? = null
) {
    private var values = mutableMapOf<String, Any?>()

    // ===== 定义 =====
    internal fun define(name: String, value: Any?) {
        values += name to value
    }

    // ===== 按距离取值 =====
    private fun at(distance: Int): Environment? =
        generateSequence(this) { it.enclosing }  // 将递归环境链从内到外压平
            .drop(distance)  // 跳指定步数
            .firstOrNull()  // 取得跳跃后的第一个，即为目标环境

    // ===== 读取值 =====
    internal operator fun get(name: Token): Any? = when {
        values.contains(name.lexeme) -> values[name.lexeme]
        enclosing != null -> enclosing[name]
        else -> throw RuntimeError(name.line, "Undefined variable '${name.lexeme}'.")
    }

    internal fun getAt(distance: Int, name: String): Any? = at(distance)
        ?.values[name]


    // ===== 写入值 =====
    internal fun assign(name: Token, value: Any?): Unit = when {
        values.contains(name.lexeme) -> values[name.lexeme] = value
        enclosing != null -> enclosing.assign(name, value)
        else -> throw RuntimeError(name.line, "Undefined variable '${name.lexeme}'.")
    }

    internal fun assignAt(distance: Int, name: Token, value: Any?) = at(distance)
        ?.values
        ?.put(name.lexeme, value)
}