package exp.compiler.klox.fronted

import exp.compiler.klox.common.LErr
import exp.compiler.klox.common.RuntimeError
import exp.compiler.klox.common.loxRequire
import exp.compiler.klox.common.stringify
import exp.compiler.klox.lang.Expr
import exp.compiler.klox.lang.Stmt
import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType

internal fun List<Stmt>.interpret() = try {
    for (stmt in this) {
        stmt.execute()
    }
} catch (e: RuntimeError) {
    LErr.runtimeError(e)
}

private fun Stmt.execute() = when (this) {
    is Stmt.Expression -> this.expression.value()
    is Stmt.Print -> println(this.expression.value().stringify())
}

private fun Expr.value(): Any? = when (this) {
    is Expr.Literal -> value
    is Expr.Grouping -> expression.value()
    is Expr.Unary -> evaluate()
    is Expr.Binary -> evaluate()
}

private fun Expr.Unary.evaluate(): Any? = when (operator.type) {
    TokenType.BANG -> !isTruthy(right.value())
    TokenType.MINUS -> {
        val rightValue = right.value()
        loxRequire(rightValue is Double, operator) {"Operands must be numbers."}
        -rightValue
    }
    else -> null
}


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

private fun evalNormalArithmetic(token: Token, leftValue: Any?, rightValue: Any?): Double? {
    loxRequire(leftValue is Double && rightValue is Double, token) {"Operands must be numbers."}

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
    loxRequire(leftValue is Double && rightValue is Double, token) {"Operands must be numbers."}

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
