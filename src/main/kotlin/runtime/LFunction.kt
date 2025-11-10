package exp.compiler.klox.runtime

import exp.compiler.klox.lang.LReturn
import exp.compiler.klox.lang.Stmt

internal class LFunction(
    private val declaration: Stmt.Function,
    private val closure : Environment,
) : LCallable {
    override val arity: Int = declaration.params.size

    context(ctx: InterpreterState)
    override fun call(arguments: List<Any?>): Any? {
        val funEnv = Environment(closure).apply {
            declaration.params
                .zip(arguments)  // 将参数一一对应压进来
                .forEach { (paramToken, argValue) ->
                    define(paramToken.lexeme, argValue)  // 这里很好地体现了为什么要区分形参和实参的概念
                }
        }

        try {
            declaration.body.executes(funEnv)
        } catch (ret: LReturn) {
            return ret.value
        }
        return null
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}