/**
 * 递归下降解析器 Recursive Descent Parser
 *
 * 规则：
 *      program        → declaration* EOF ;
 *
 *      declaration    → funDecl | varDecl | statement ;
 *      funDecl        → "fun" function ;
 *      function       → IDENTIFIER "(" parameters? ")" block ;
 *      parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
 *      varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 *      statement      → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block;
 *      forStmt        → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
 *      block          → "{" declaration* "}" ;
 *      ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
 *      printStmt      → "print" expression ";" ;
 *      returnStmt     → "return" expression? ";" ;
 *      whileStmt      → "while" "(" expression ")" statement ;
 *      expression     → assignment ;
 *      assignment     → IDENTIFIER "=" assignment | logic_or ;
 *      logic_or       → logic_and ( "or" logic_and )* ;
 *      logic_and      → equality ( "and" equality )* ;
 *      equality       → comparison ( ( "!=" | "==" ) comparison )*
 *      comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 *      term           → factor ( ( "-" | "+" ) factor )*
 *      factor         → unary ( ( "/" | "*" ) unary )*
 *      unary          → ( "!" | "-" ) unary | call ;
 *      call           → primary ( "(" arguments? ")" )* ;
 *      arguments      → expression ( "," expression )* ;
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
            while (!isAtEnd()) declaration()?.let { add(it) }
        }
    }

private class ParserState(
    val tokens: List<Token>
) {
    var current = 0
}

/**
 * declaration    → funDecl | varDecl | statement ;
 */
private fun ParserState.declaration(): Stmt? = try {
    when {
        match(TokenType.VAR) -> varDeclaration()
        match(TokenType.FUN) -> function("function")
        else -> statement()
    }
} catch (_: ParseError) {
    synchronize()
    null
}

/**
 * funDecl        → "fun" function ;
 * function       → IDENTIFIER "(" parameters? ")" block ;
 */
private fun ParserState.function(kind: String): Stmt {
    val funName = consumeOrErr(TokenType.IDENTIFIER, "Expect $kind name.")

    consumeOrErr(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
    val params =
        if (check(TokenType.RIGHT_PAREN)) emptyList()
        else parameters()
    consumeOrErr(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

    consumeOrErr(TokenType.LEFT_BRACE, "Expect '{' after $kind name.")
    val logic = block()

    return Stmt.Function(funName, params, logic)
}

/**
 * parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
 */
private fun ParserState.parameters(): List<Token> =
    generateSequence(consumeOrErr(TokenType.IDENTIFIER, "Expect function parameters.")) {
        if (!match(TokenType.COMMA)) null
        else consumeOrErr(TokenType.IDENTIFIER, "Expect function parameters after ','.")
    }.toList()

/**
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 */
private fun ParserState.varDeclaration(): Stmt {
    val name = consumeOrErr(TokenType.IDENTIFIER, "Illegal first parameter(unexpected).")

    val initializer: Expr? =
        if (match(TokenType.EQUAL)) {
            expression()
        } else null

    consumeOrErr(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return Stmt.Var(name, initializer)
}

/**
 * statement      → exprStmt | forSmt | ifStmt | printStmt | whileStmt | block;
 */
private fun ParserState.statement(): Stmt = when {
    match(TokenType.PRINT) -> printStatement()
    match(TokenType.IF) -> ifStatement()
    match(TokenType.WHILE) -> whileStatement()
    match(TokenType.FOR) -> forStatement()
    match(TokenType.RETURN) -> returnStatement()
    match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
    else -> expressionStatement()
}

/**
 * returnStmt     → "return" expression? ";" ;
 */

private fun ParserState.returnStatement(): Stmt {
    val keyword = previous()

    val value = if (!check(TokenType.SEMICOLON)) expression() else null
    consumeOrErr(TokenType.SEMICOLON, "Expect ';' after return value.")

    return Stmt.Return(keyword, value)
}

/**
 * forStmt        → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
 */
private fun ParserState.forStatement(): Stmt {
    consumeOrErr(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

    val initializer = when {
        match(TokenType.SEMICOLON) -> null
        match(TokenType.VAR) -> varDeclaration()
        else -> expressionStatement()
    }

    val condition = if (!check(TokenType.SEMICOLON)) expression() else null
    consumeOrErr(TokenType.SEMICOLON, "Expect ';' after for condition.")

    val increment = if (!check(TokenType.RIGHT_PAREN)) expression() else null
    consumeOrErr(TokenType.RIGHT_PAREN, "Expect ')' after for clause.")

    /**
     * {
     *     initializer;
     *     while (condition) {
     *         body;
     *         increment;
     *     }
     * }
     */
    return Stmt.Block(
        listOfNotNull(
            initializer,
            Stmt.While(
                condition ?: Expr.Literal(true),
                Stmt.Block(
                    listOfNotNull(
                        statement(),
                        increment?.let { Stmt.Expression(it) }
                    ))
            )
        ))
}

/**
 * whileStmt      → "while" "(" expression ")" statement ;
 */
private fun ParserState.whileStatement(): Stmt {
    consumeOrErr(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
    val condition = expression()
    consumeOrErr(TokenType.RIGHT_PAREN, "Expect ')' after while condition.")

    val body = statement()

    return Stmt.While(condition, body)
}

/**
 * ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
 */
private fun ParserState.ifStatement(): Stmt {
    consumeOrErr(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
    val condition = expression()
    consumeOrErr(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

    val thenBranch = statement()

    val elseBranch = if (match(TokenType.ELSE)) statement() else null

    return Stmt.If(condition, thenBranch, elseBranch)
}

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
 * assignment     → IDENTIFIER "=" assignment | logic_or ;
 */
private fun ParserState.assignment(): Expr {
    val expr = or()

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
 * logic_or       → logic_and ( "or" logic_and )* ;
 */
private fun ParserState.or(): Expr = parseLeftAssociative(
    TokenType.OR
) { and() }

/**
 * logic_and      → equality ( "and" equality )* ;
 */

private fun ParserState.and(): Expr = parseLeftAssociative(
    TokenType.AND
) { equality() }

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
 * unary          → ( "!" | "-" ) unary | call ;
 */
private fun ParserState.unary(): Expr {
    if (match(
            TokenType.BANG,
            TokenType.MINUS,
        )
    ) {
        val operator = previous()
        val right = unary()
        return Expr.Unary(operator, right)
    }
    return call()
}

/**
 * call           → primary ( "(" arguments? ")" )* ;
 */
private fun ParserState.call(): Expr =
    generateSequence(primary()) { currentExpr ->
        if (match(TokenType.LEFT_PAREN)) {
            currentExpr.finishCall()
        } else null
    }.last()

/**
 * arguments      → expression ( "," expression )* ;
 */
context(state: ParserState)
private fun Expr.finishCall(): Expr {
    val args: List<Expr> =
        if (state.check(TokenType.RIGHT_PAREN)) emptyList()
        else state.arguments()

    val paren = state.consumeOrErr(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

    return Expr.Call(this, paren, args)
}

private tailrec fun ParserState.arguments(head: List<Expr> = emptyList()): List<Expr> {
    // 限制参数不超过 255 个（放这里是为了快速错误）
    if (head.size >= 255) LErr.error(peek(), "Can't have more than 255 arguments.")

    val currentArgs = head + expression()

    return if (match(TokenType.COMMA)) arguments(currentArgs)
    else currentArgs
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

private fun ParserState.peek(): Token = tokens[current]

private fun ParserState.previous(): Token = tokens[current - 1]

private fun ParserState.advance(): Token {
    if (!isAtEnd()) current++
    return previous()
}

private fun ParserState.consumeOrErr(type: TokenType, message: String): Token {
    if (check(type)) return advance()
    throw parseError(peek(), message)
}

private fun ParserState.check(type: TokenType): Boolean =
    if (isAtEnd()) false
    else peek().type == type

private fun ParserState.isAtEnd(): Boolean = peek().type == TokenType.EOF

private fun ParserState.parseLeftAssociative(vararg operators: TokenType, rule: () -> Expr): Expr {
    var expr: Expr = rule()
    while (match(*operators)) {
        val operator = previous()
        val right = rule()
        expr = Expr.Binary(expr, operator, right)
    }
    return expr
}

// 错误处理

private fun parseError(token: Token, message: String): ParseError {
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