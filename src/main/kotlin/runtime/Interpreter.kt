package exp.compiler.klox.runtime

import exp.compiler.klox.common.LErr
import exp.compiler.klox.common.RuntimeError
import exp.compiler.klox.common.loxRequire
import exp.compiler.klox.common.stringify
import exp.compiler.klox.lang.*

internal data class InterpreterContext(var environment: Environment)

internal fun List<Stmt>.interpret(globalEnv: Environment) = try {
    with(InterpreterContext(globalEnv)) {
        for (stmt in this@interpret) {
            stmt.execute()
        }
    }
} catch (e: RuntimeError) {
    LErr.runtimeError(e)
}

context(ctx: InterpreterContext)
private fun Stmt.execute() {
    when (this) {
        is Stmt.Expression -> this.expression.value()

        is Stmt.Print -> print(this.expression.value().stringify())

        is Stmt.Var -> ctx.environment.define(name.lexeme, initializer?.value())

        is Stmt.Block -> statements.executes(Environment(enclosing = ctx.environment))

        is Stmt.If -> when {
            isTruthy(condition.value()) -> thenBranch.execute()
            elseBranch != null -> elseBranch.execute()
            else -> {}
        }

        is Stmt.While -> while (isTruthy(condition.value())) body.execute()

        is Stmt.Function -> ctx.environment.define(
            name.lexeme,
            LFunction(this, ctx.environment)
        )

        is Stmt.Return -> throw LReturn(value?.value())
    }
}

context(ctx: InterpreterContext)
internal fun List<Stmt>.executes(scopedEnvironment: Environment) {
    val previous = ctx.environment // 保存旧环境
    try {
        ctx.environment = scopedEnvironment // 进入新作用域
        for (statement in this) {
            statement.execute() // 在新环境中执行
        }
    } finally {
        ctx.environment = previous // 保证恢复旧环境
    }
}

context(ctx: InterpreterContext)
private fun Expr.value(): Any? = when (this) {
    is Expr.Literal -> value
    is Expr.Grouping -> expression.value()
    is Expr.Unary -> evaluate()
    is Expr.Binary -> evaluate()
    is Expr.Assign -> evaluate()
    is Expr.Variable -> ctx.environment.get(name)
    is Expr.Logical -> evaluate()
    is Expr.Call -> evaluate()
}

context(ctx: InterpreterContext)
private fun Expr.Call.evaluate() = callee.value().run {
    when (this) {
        is LCallable -> {
            if (arguments.size != arity) throw RuntimeError(
                paren.line,
                "Expected $arity arguments but got ${arguments.size} ."
            )
            call(arguments.map { it.value() })
        }

        else -> throw RuntimeError(paren.line, "Can only call functions and classes.")
    }
}


context(ctx: InterpreterContext)
private fun Expr.Unary.evaluate(): Any? = when (operator.type) {
    TokenType.BANG -> !isTruthy(right.value())
    TokenType.MINUS -> {
        val rightValue = right.value()
        loxRequire(rightValue is Double, operator) { "Operands must be numbers." }
        -rightValue
    }

    else -> null
}

context(ctx: InterpreterContext)
private fun Expr.Binary.evaluate(): Any? = when (this.operator.type) {
    // --- 算术运算 ---
    TokenType.MINUS, TokenType.STAR, TokenType.SLASH ->
        evalNormalArithmetic(operator, left.value(), right.value())
    // --- 特殊的"+"运算 ---
    TokenType.PLUS ->
        evalPlus(operator, left.value(), right.value())
    // --- 比较运算 ---
    TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL ->
        evalCompare(operator, left.value(), right.value())
    // --- 相等性判断 ---
    TokenType.BANG_EQUAL -> !isEqual(left.value(), right.value())
    TokenType.EQUAL_EQUAL -> isEqual(left.value(), right.value())
    else -> null
}

context(ctx: InterpreterContext)
private fun Expr.Assign.evaluate(): Any? {
    val calculatedValue = value.value()
    ctx.environment.assign(name, calculatedValue)
    return calculatedValue
}

context(ctx: InterpreterContext)
private fun Expr.Logical.evaluate(): Any? {
    val leftValue = left.value()

    if (logicOp.type == TokenType.OR) {
        if (isTruthy(leftValue)) return leftValue
    } else { // AND
        if (!isTruthy(leftValue)) return leftValue
    }

    return right.value()
}


private fun evalNormalArithmetic(token: Token, leftValue: Any?, rightValue: Any?): Double? {
    loxRequire(leftValue is Double && rightValue is Double, token) { "Operands must be numbers." }

    return when (token.type) {
        TokenType.MINUS -> leftValue - rightValue
        TokenType.STAR -> leftValue * rightValue
        TokenType.SLASH -> {
            if (rightValue == 0.0) throw RuntimeError(token.line, "Division by zero.")
            leftValue / rightValue
        }

        else -> null
    }
}

private fun evalPlus(token: Token, leftValue: Any?, rightValue: Any?): Any = when (leftValue) {
    is Double if rightValue is Double -> leftValue + rightValue
    is String if rightValue is String -> leftValue + rightValue
    else -> throw RuntimeError(token.line, "Operands must be two numbers or two strings.")
}

private fun evalCompare(token: Token, leftValue: Any?, rightValue: Any?): Boolean? {
    loxRequire(leftValue is Double && rightValue is Double, token) { "Operands must be numbers." }

    return when (token.type) {
        TokenType.GREATER -> leftValue > rightValue
        TokenType.GREATER_EQUAL -> leftValue >= rightValue
        TokenType.LESS -> leftValue < rightValue
        TokenType.LESS_EQUAL -> leftValue <= rightValue
        else -> null // 不可达
    }
}

/**
 * 对真值性和假值性的判断。
 * 规则来自 Ruby —— false 和 nil 被视为 falsey，其它所有值都被视为 truthy。
 * @param value 待判断的 Any 值
 */
private fun isTruthy(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    else -> true
}

/**
 * 对相等性的判断规则。
 */
private fun isEqual(a: Any?, b: Any?): Boolean = when (a) {
    null if b == null -> true
    null -> false
    else -> a == b
}
