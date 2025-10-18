package exp.compiler.klox.fronted

import exp.compiler.klox.common.LErr
import exp.compiler.klox.common.RuntimeError
import exp.compiler.klox.lang.Expr
import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType

internal fun Expr.interpret(): Any? = try {
    when (this) {
        is Expr.Literal -> this.value

        is Expr.Grouping -> this.expression.interpret()

        is Expr.Unary -> {
            val right = this.right.interpret()

            when (this.operator.type) {
                TokenType.BANG -> !isTruthy(right)
                TokenType.MINUS -> {
                    checkNumberOperand(this.operator, right)
                    -(right as Double)
                }

                else -> null
            }
        }

        is Expr.Binary -> {
            val left = this.left.interpret()
            val right = this.right.interpret()

            when (this.operator.type) {
                // --- 算术运算 ---
                TokenType.MINUS, TokenType.STAR, TokenType.SLASH -> {
                    checkNumberOperands(this.operator, left, right)
                    val leftDouble = left as Double
                    val rightDouble = right as Double

                    when (this.operator.type) {
                        TokenType.MINUS -> leftDouble - rightDouble
                        TokenType.STAR -> leftDouble * rightDouble
                        TokenType.SLASH -> {
                            if (rightDouble == 0.0) throw RuntimeError(this.operator, "Division by zero.")
                            leftDouble / rightDouble
                        }

                        else -> null
                    }
                }
                // --- 特殊的"+"运算 ---
                TokenType.PLUS -> when (left) {
                    is Double if right is Double -> left + right
                    is String if right is String -> left + right
                    else -> throw RuntimeError(this.operator, "Operands must be two numbers or two strings.")
                }
                // --- 比较运算 ---
                TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL -> {
                    checkNumberOperands(this.operator, left, right)
                    val leftDouble = left as Double
                    val rightDouble = right as Double

                    when (this.operator.type) {
                        TokenType.GREATER -> leftDouble > rightDouble
                        TokenType.GREATER_EQUAL -> leftDouble >= rightDouble
                        TokenType.LESS -> leftDouble < rightDouble
                        TokenType.LESS_EQUAL -> leftDouble <= rightDouble
                        else -> null // 不可达
                    }
                }
                // --- 相等性判断 ---
                TokenType.BANG_EQUAL -> !isEqual(left, right)
                TokenType.EQUAL_EQUAL -> isEqual(left, right)

                else -> null  //
            }
        }
    }
} catch (e: RuntimeError) {
    LErr.runtimeError(e)
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

/**
 * 辅助函数：检查单个操作数是否为数字
 */
private fun checkNumberOperand(operator: Token, operand: Any?) {
    if (operand is Double) return
    throw RuntimeError(operator, "Operand must be a number.")
}

/**
 * 辅助函数：检查两个操作数是否都为数字
 */
private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
    if (left is Double && right is Double) return
    throw RuntimeError(operator, "Operands must be numbers.")
}
