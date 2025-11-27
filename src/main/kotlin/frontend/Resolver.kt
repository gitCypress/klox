/**
 * 该 Resolver 目前只涉及对变量的静态分析
 */

package exp.compiler.klox.frontend

import exp.compiler.klox.common.LErr
import exp.compiler.klox.lang.Expr
import exp.compiler.klox.lang.Stmt
import exp.compiler.klox.lang.Token
import kotlin.collections.forEach

// ===== External Function =====

internal fun List<Stmt>.resolve(): MutableMap<Expr, Int> =
    with(ResolverState()) {
        resolveStatements(this@resolve)
        localsResult
    }

// ===== Resolver =====

/**
 * Stmt 中：
 * 涉及作用域变化的：Block、Function
 * 涉及变量定义的：Block、Function、Var
 */

context(rState: ResolverState)
private fun Stmt.resolve(): Unit = when (this) {
    is Stmt.Block -> {
        rState.beginScope()
        resolveStatements(statements)
        rState.endScope()
    }

    is Stmt.Expression -> expression.resolve()
    is Stmt.Function -> {
        // 这里为了递归正常工作，声明完就立即定义了
        rState.declare(name)
        rState.define(name)
        resolveFunction(params, body, FunctionType.FUNCTION)
    }

    is Stmt.If -> {
        condition.resolve()
        thenBranch.resolve()
        elseBranch?.resolve() ?: Unit
    }

    is Stmt.Print -> expression.resolve()
    is Stmt.Return -> value?.resolve() ?: Unit
    is Stmt.Var -> {
        rState.declare(name)
        initializer?.resolve()
        rState.define(name)
    }

    is Stmt.While -> {
        condition.resolve()
        body.resolve()
    }
}

context(rState: ResolverState)
private fun resolveStatements(stmts: List<Stmt>) {
    stmts.forEach { it.resolve() }
}

context(rState: ResolverState)
private fun resolveFunction(params : List<Token>, body : List<Stmt>, type: FunctionType) {
    val enclosingFunction = rState.currentFunType
    rState.currentFunType = type

    rState.beginScope()
    params.forEach {
        rState.declare(it)
        rState.define(it)
    }
    body.resolve()
    rState.endScope()

    rState.currentFunType = enclosingFunction
}

/**
 * Expr 中：
 * 涉及变量分析的：Assign、Variable
 * Assign 是写变量用的，Variable 是读变量用的，二者都是变量名绑定操作
 * localResults 只对变量名绑定操作有意义
 */

context(rState: ResolverState)
private fun Expr.resolve(): Unit = when (this) {
    is Expr.Assign -> {
        value.resolve()
        resolveLocal(this, name)
    }

    is Expr.Binary -> {
        left.resolve()
        right.resolve()
    }

    is Expr.Call -> {
        callee.resolve()
        resolveArguments(arguments)
    }

    is Expr.Grouping -> expression.resolve()
    is Expr.Literal -> {}
    is Expr.Logical -> {
        left.resolve()
        right.resolve()
    }

    is Expr.Unary -> right.resolve()
    is Expr.Variable -> {
        if (
            rState.scopes.isNotEmpty()
            && rState.scopes.last()[name.lexeme] == VarStat.DECLARED
        ) {
            // 避免 var a = a; 的写法，这会导致 a = nil
            // 具体见 Stmt.resolve()
            LErr.error(name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(this, name)
    }
}

context(rState: ResolverState)
private fun resolveLocal(expr: Expr, name: Token) {
    rState.scopes
        .asReversed()
        .withIndex()
        .firstNotNullOfOrNull { (depth, scope) ->
            depth.takeIf { scope.containsKey(name.lexeme) }
        }
        ?.let {
            rState.localsResult.put(expr, it)
        }
}

context(rState: ResolverState)
private fun resolveArguments(args: List<Expr>) {
    args.forEach { it.resolve() }
}

// ===== ResolverState =====

private enum class VarStat {
    DECLARED,
    DEFINED,
}

private enum class FunctionType {
    NONE,
    FUNCTION
}

private class ResolverState() {
    val scopes: ArrayDeque<MutableMap<String, VarStat>> = ArrayDeque()  // 在解析时模拟运行时的环境链
    var currentFunType: FunctionType = FunctionType.NONE

    val localsResult: MutableMap<Expr, Int> = mutableMapOf()  // 存储解析结果
}

private fun ResolverState.beginScope() {
    scopes.add(mutableMapOf())
}

private fun ResolverState.endScope() {
    scopes.removeLast()
}

private fun ResolverState.declare(name: Token) {
    scopes
        .lastOrNull()
        ?.apply {
            if (containsKey(name.lexeme)) {
                LErr.error(name, "Already a variable with this name in this scope.")
            }
            put(name.lexeme, VarStat.DECLARED)
        }
}

private fun ResolverState.define(name: Token) {
    scopes
        .lastOrNull()
        ?.apply {
            put(name.lexeme, VarStat.DEFINED)
        }
}
