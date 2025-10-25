/**
 * 递归下降解析器 Recursive Descent Parser
 *
 * 规则：
 *      program        → declaration* EOF ;
 *
 *      declaration    → varDecl | statement ;
 *      varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 *      statement      → exprStmt | printStmt | block;
 *      block          → "{" declaration* "}" ;
 *      printStmt      → "print" expression ";" ;
 *      expression     → assignment ;
 *      assignment     → IDENTIFIER "=" assignment | equality ;
 *      equality       → comparison ( ( "!=" | "==" ) comparison )*
 *      comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 *      term           → factor ( ( "-" | "+" ) factor )*
 *      factor         → unary ( ( "/" | "*" ) unary )*
 *      unary          → ( "!" | "-" | "+") unary | primary
 *      primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER
 */

package exp.compiler.klox.frontend

import exp.compiler.klox.common.LErr
import exp.compiler.klox.common.ParseError
import exp.compiler.klox.lang.Expr
import exp.compiler.klox.lang.Stmt
import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType

internal fun List<Token>.parse(): List<Stmt> =
    ParserState(this).run {
        buildList {
            // TODO: 这里符合 synchronize() 函数的设计思路吗
            while (!isAtEnd()) declaration()?.let { add(it) }
        }
    }

private class ParserState(
    val tokens: List<Token>
) {
    var current = 0
}

/**
 * declaration    → varDecl | statement ;
 */
private fun ParserState.declaration(): Stmt? = try {
    if (match(TokenType.VAR)) varDeclaration()
    else statement()
} catch (_: ParseError) {
    synchronize()
    null
}

/**
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 */
private fun ParserState.varDeclaration(): Stmt {
    val name = consumeOrErr(TokenType.IDENTIFIER, "Expect variable name.")

    val initializer: Expr? =
        if (match(TokenType.EQUAL)) {
            expression()
        } else null

    consumeOrErr(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return Stmt.Var(name, initializer)
}

/**
 * statement      → exprStmt | printStmt | block;
 */
private fun ParserState.statement(): Stmt =
    if (match(TokenType.PRINT)) printStatement()
    else if (match(TokenType.LEFT_BRACE)) Stmt.Block(block())
    else expressionStatement()

/**
 * block          → "{" declaration* "}" ;
 */
private fun ParserState.block(): List<Stmt> {
    val statements = buildList {
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) declaration()?.let { add(it) }
    }
    consumeOrErr(TokenType.RIGHT_BRACE, "Expect '}' after expression.")
    return statements
}

/**
 * printStmt      → "print" expression ";" ;
 */
private fun ParserState.printStatement(): Stmt.Print {
    val value = expression()
    consumeOrErr(TokenType.SEMICOLON, "Expect ';' after value.")
    return Stmt.Print(value)
}

/**
 * exprStmt       → expression ";" ;
 */
private fun ParserState.expressionStatement(): Stmt.Expression {
    val value = expression()
    consumeOrErr(TokenType.SEMICOLON, "Expect ';' after expression.")
    return Stmt.Expression(value)
}

/**
 * expression     → assignment ;
 */
private fun ParserState.expression(): Expr = assignment()

/**
 * assignment     → IDENTIFIER "=" assignment | equality ;
 */
private fun ParserState.assignment(): Expr {
    val expr = equality()

    if (match(TokenType.EQUAL)) {
        val equals = previous()
        val value = assignment()

        if (expr is Expr.Variable) {
            val name = expr.name
            return Expr.Assign(name, value)
        }

        LErr.error(equals, "Invalid assignment target.")
    }

    return expr
}

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
 * primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER;
 */
private fun ParserState.primary(): Expr = when {
    match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
    match(TokenType.TRUE) -> Expr.Literal(true)
    match(TokenType.FALSE) -> Expr.Literal(false)
    match(TokenType.NIL) -> Expr.Literal(null)
    match(TokenType.LEFT_PAREN) -> {
        val expr = expression()
        consumeOrErr(TokenType.RIGHT_PAREN, "Expected ')' after expression")
        Expr.Grouping(expr)
    }

    match(TokenType.IDENTIFIER) -> Expr.Variable(previous())

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

private fun ParserState.check(type: TokenType): Boolean =
    if (isAtEnd()) false
    else peek().type == type


private fun ParserState.isAtEnd(): Boolean = peek().type == TokenType.EOF
private fun ParserState.peek(): Token = tokens[current]
private fun ParserState.previous(): Token = tokens[current - 1]

private fun ParserState.consumeOrErr(type: TokenType, message: String): Token {
    if (check(type)) return advance()
    throw parseError(peek(), message)
}

private fun ParserState.parseLeftAssociative(vararg operators: TokenType, rule: () -> Expr): Expr {
    var expr: Expr = rule()
    while (match(*operators)) {
        val operator = previous()
        val right = rule()
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