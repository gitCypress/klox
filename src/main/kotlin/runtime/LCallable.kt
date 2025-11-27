package exp.compiler.klox.runtime

internal interface LCallable {
    val arity: Int  // 参数数量

    context(iState: InterpreterState)
    fun call(arguments: List<Any?>): Any?
}