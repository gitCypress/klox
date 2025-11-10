package exp.compiler.klox.frontend

import exp.compiler.klox.common.LErr
import exp.compiler.klox.lang.Expr
import exp.compiler.klox.lang.Stmt
import exp.compiler.klox.lang.Token

internal fun List<Stmt>.resolve(): MutableMap<Expr, Int> = with(ResolverState()) {
    for (statement in this@resolve) {
        statement.resolve()
    }
    localsResult
}

private enum class VarStat {
    DECLARED,
    DEFINED,
}

private enum class FunctionType {
    NONE,
    FUNCTION
}


private class ResolverState() {
    val scopes: ArrayDeque<MutableMap<String, VarStat>> = ArrayDeque()
    var currentFunction: FunctionType = FunctionType.NONE
    val localsResult: MutableMap<Expr, Int> = mutableMapOf()
}

context(rState: ResolverState)
private fun Stmt.resolve(): Unit = when (this) {
    is Stmt.Block -> {
        rState.beginScope()
        statements.resolveStatements()
        rState.endScope()
    }

    is Stmt.Expression -> expression.resolve()
    is Stmt.Function -> {
        rState.declare(name)
        rState.define(name)
        resolveFunction(FunctionType.FUNCTION)
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
private fun Expr.resolve(): Unit = when (this) {
    is Expr.Assign -> {
        value.resolve()
        resolveLocal(name)
    }

    is Expr.Binary -> {
        left.resolve()
        right.resolve()
    }

    is Expr.Call -> {
        callee.resolve()
        arguments.forEach { it.resolve() }
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
            LErr.error(name, "Can't read local variable in its own initializer.")
        }
        resolveLocal(name)
    }
}

private fun ResolverState.beginScope() {
    this.scopes.add(mutableMapOf())
}

private fun ResolverState.endScope() {
    this.scopes.removeLast()
}

private fun ResolverState.declare(name: Token) {
    scopes.lastOrNull()?.apply {
        if (containsKey(name.lexeme)) {
            LErr.error(name, "Already a variable with this name in this scope.")
        }
        put(name.lexeme, VarStat.DECLARED)
    }
}

private fun ResolverState.define(name: Token) {
    scopes.lastOrNull()?.let { it[name.lexeme] = VarStat.DEFINED }
}

context(rState: ResolverState)
private fun List<Stmt>.resolveStatements() {
    for (statement in this) {
        statement.resolve()
    }
}

context(rState: ResolverState)
private fun Expr.resolveLocal(name: Token) {
    rState.scopes
        .asReversed()
        .withIndex()
        .firstNotNullOfOrNull { (depth, scope) ->
            depth.takeIf { scope.containsKey(name.lexeme) }
        }
        ?.let { rState.localsResult.put(this, it) }
}

context(rState: ResolverState)
private fun Stmt.Function.resolveFunction(type: FunctionType) {
    val enclosingFunction = rState.currentFunction
    rState.currentFunction = type

    rState.beginScope()
    params.forEach {
        rState.declare(it)
        rState.define(it)
    }
    body.resolve()
    rState.endScope()

    rState.currentFunction = enclosingFunction
}

