/**
 * 递归下降解析器 Recursive Descent Parser
 *
 * 规则：
 *      expression     → equality
 *      equality       → comparison ( ( "!=" | "==" ) comparison )*
 *      comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 *      term           → factor ( ( "-" | "+" ) factor )*
 *      factor         → unary ( ( "/" | "*" ) unary )*
 *      unary          → ( "!" | "-" | "+") unary | primary
 *      primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
 */

package exp.compiler.klox.fronted

import exp.compiler.klox.common.LErr
import exp.compiler.klox.common.ParseError
import exp.compiler.klox.lang.Expr
import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType

internal fun List<Token>.parse(): Expr? = try {
    ParserState(this).expression()
} catch (_: ParseError) {
    null
}

private class ParserState(
    val tokens: List<Token>
) {
    var current = 0
}

/**
 * expression     → equality ;
 */
private fun ParserState.expression(): Expr = equality()

/**
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 */
private fun ParserState.equality(): Expr = parseLeftAssociative(
    TokenType.BANG_EQUAL,
    TokenType.EQUAL_EQUAL,
) { comparison() }

/**
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 */
private fun ParserState.comparison(): Expr = parseLeftAssociative(
    TokenType.GREATER,
    TokenType.GREATER_EQUAL,
    TokenType.LESS,
    TokenType.LESS_EQUAL,
) { term() }

/**
 * term           → factor ( ( "-" | "+" ) factor )* ;
 */
private fun ParserState.term(): Expr = parseLeftAssociative(
    TokenType.PLUS,
    TokenType.MINUS,
) { factor() }

/**
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 */
private fun ParserState.factor(): Expr = parseLeftAssociative(
    TokenType.STAR, TokenType.SLASH,
) { unary() }

/**
 * unary          → ( "!" | "-" | "+") unary | primary ;
 */
private fun ParserState.unary(): Expr {
    if (match(
            TokenType.BANG,
            TokenType.MINUS,
            TokenType.PLUS,
        )
    ) {
        val operator = previous()
        val right = unary()
        return Expr.Unary(operator, right)
    }
    return primary()
}

/**
 * primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
 */
private fun ParserState.primary(): Expr = when {
    match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
    match(TokenType.TRUE) -> Expr.Literal(true)
    match(TokenType.FALSE) -> Expr.Literal(false)
    match(TokenType.NIL) -> Expr.Literal(null)
    match(TokenType.LEFT_PAREN) -> {
        val expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
        Expr.Grouping(expr)
    }

    else -> {
        throw parseError(peek(), "Unexpected expression.")
    }
}

// 辅助函数

private fun ParserState.match(vararg types: TokenType): Boolean {
    for (type in types) {
        if (check(type)) {
            advance()
            return true
        }
    }
    return false
}

private fun ParserState.advance(): Token {
    if (!isAtEnd()) current++
    return previous()
}

private fun ParserState.check(type: TokenType): Boolean {
    if (isAtEnd()) return false
    return peek().type == type
}

private fun ParserState.isAtEnd(): Boolean = peek().type == TokenType.EOF
private fun ParserState.peek(): Token = tokens[current]
private fun ParserState.previous(): Token = tokens[current - 1]

private fun ParserState.consume(type: TokenType, message: String): Token {
    if (check(type)) return advance()
    throw parseError(peek(), message)
}

private fun ParserState.parseLeftAssociative(vararg operators: TokenType, expression: () -> Expr): Expr {
    var expr: Expr = expression()
    while (match(*operators)) {
        val operator = previous()
        val right = expression()
        expr = Expr.Binary(expr, operator, right)
    }
    return expr
}

/**
 * 错误处理
 */

private fun ParserState.parseError(token: Token, message: String): ParseError {
    LErr.error(token, message)
    return ParseError()
}

private fun ParserState.synchronize() {
    advance()

    while (!isAtEnd()) {
        if (previous().type == TokenType.SEMICOLON) return

        when (peek().type) {
            TokenType.CLASS, TokenType.FUN, TokenType.VAR,
            TokenType.FOR, TokenType.IF, TokenType.WHILE,
            TokenType.PRINT, TokenType.RETURN
                -> return

            else -> {}
        }
        advance()
    }
}