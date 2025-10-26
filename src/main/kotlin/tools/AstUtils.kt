package exp.compiler.klox.tools

import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType
import exp.compiler.klox.lang.Expr

internal fun Expr.toRPN(): String =
    when (this) {
        is Expr.Binary -> "${left.toRPN()} ${right.toRPN()} ${operator.lexeme}"
        is Expr.Grouping -> expression.toRPN()
        is Expr.Literal -> value?.toString() ?: "nil"
        is Expr.Unary -> "${operator.lexeme}${right.toRPN()}"
        else -> "unsupported"
    }

internal fun Expr.toAST(): String {
    fun parenthesize(name: String, vararg exprs: Expr): String =
        "($name${exprs.joinToString(prefix = " ", separator = " ") { it.toAST() }})"

    return when (this) {
        is Expr.Binary -> parenthesize(operator.lexeme, left, right)
        is Expr.Grouping -> parenthesize("group", expression)
        is Expr.Literal -> value?.toString() ?: "nil"
        is Expr.Unary -> parenthesize(operator.lexeme, right)
        else -> "unsupported"
    }
}

internal data object ExampleExpr {
    //
    val exp1: Expr = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(
            Expr.Literal(45.67)
        )
    )

    val exp2 = Expr.Binary(
        Expr.Binary(
            Expr.Unary(
                Token(TokenType.MINUS, "-", null, 1),
                Expr.Literal(1)
            ),
            Token(TokenType.PLUS, "+", null, 1),
            Expr.Literal(2),
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Binary(
            Expr.Literal(4),
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(3),
        ),
    )
}


fun main() {
    println(ExampleExpr.exp1.toAST())
    println(ExampleExpr.exp2.toRPN())
}
