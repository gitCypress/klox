package exp.compiler.klox

internal class Parser(val tokens: List<Token>) {
    private var current: Int = 0

    fun parse(): Expr? {
        return try {
            expression()
        } catch (_: ParseError) {
            null
        }
    }

    /**
     * expression     → equality ;
     */
    private fun expression(): Expr = equality()

    /**
     * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private fun equality(): Expr = parseLeftAssociative(
        TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL
    ) { comparison() }

    /**
     * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     */
    fun comparison(): Expr = parseLeftAssociative(
        TokenType.GREATER,
        TokenType.GREATER_EQUAL,
        TokenType.LESS,
        TokenType.LESS_EQUAL,
    ) { term() }

    /**
     * term           → factor ( ( "-" | "+" ) factor )* ;
     */
    private fun term(): Expr = parseLeftAssociative(
        TokenType.PLUS, TokenType.MINUS,
    ) { factor() }

    /**
     * factor         → unary ( ( "/" | "*" ) unary )* ;
     */
    private fun factor(): Expr = parseLeftAssociative(
        TokenType.STAR, TokenType.SLASH,
    ) { unary() }

    /**
     * unary          → ( "!" | "-" | "+") unary
     *                | primary ;
     */
    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = expression()
            return Expr.Unary(operator, right)
        }
        return primary()
    }

    /**
     * primary        → NUMBER | STRING | "true" | "false" | "nil"
     *                | "(" expression ")" ;
     */
    private fun primary(): Expr = when {
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
            throw error(peek(), "Unexpected expression.")
        }
    }

    /**
     * 辅助函数
     */

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun parseLeftAssociative(vararg operators: TokenType, expression: () -> Expr): Expr {
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

    class ParseError : RuntimeException()

    private fun error(token: Token, message: String): ParseError {
        LErr.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE,
                TokenType.PRINT, TokenType.RETURN
                    -> return

                else -> {}
            }

            advance()
        }
    }


}