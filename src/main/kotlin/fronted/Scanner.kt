package exp.compiler.klox.fronted

import exp.compiler.klox.common.LErr
import exp.compiler.klox.common.isIdentifierPart
import exp.compiler.klox.common.isIdentifierStart
import exp.compiler.klox.lang.Token
import exp.compiler.klox.lang.TokenType

internal fun String.scan(): List<Token> = sequence {
    val state = ScannerState(this@scan)
    while (true) {
        val token = state.scanToken() ?: continue
        yield(token)
        if (token.type == TokenType.EOF) break
    }
}.toList()

private class ScannerState(
    val source: String
) {
    var line = 1  // current 所指所在代码行
    var start = 0  // 指向正在扫描的词素中的第一个字符
    var current = 0  // 指向当前正在考虑的字符
}

/**
 * 扫描单个 token
 * @return 返回 null 代表本次扫描内容无效（内容无用或者有错误），EOF代表扫描结束，其他为一般Token
 */
private fun ScannerState.scanToken(): Token? {
    start = current
    val c = advance() ?: return Token(TokenType.EOF, "", null, line)
    return when (c) {
        // single
        '(' -> Token(TokenType.LEFT_PAREN, "(", null, line)
        ')' -> Token(TokenType.RIGHT_PAREN, ")", null, line)
        '{' -> Token(TokenType.LEFT_BRACE, "{", null, line)
        '}' -> Token(TokenType.RIGHT_BRACE, "}", null, line)
        ',' -> Token(TokenType.COMMA, ",", null, line)
        '.' -> Token(TokenType.DOT, ".", null, line)
        '-' -> Token(TokenType.MINUS, "-", null, line)
        '+' -> Token(TokenType.PLUS, "+", null, line)
        ';' -> Token(TokenType.SEMICOLON, ";", null, line)
        '*' -> Token(TokenType.STAR, "*", null, line)

        // double
        '!' ->
            if (match('=')) Token(TokenType.BANG_EQUAL, "!=", null, line)
            else Token(TokenType.BANG, "!", null, line)

        '=' ->
            if (match('=')) Token(TokenType.EQUAL_EQUAL, "==", null, line)
            else Token(TokenType.EQUAL, "=", null, line)

        '<' ->
            if (match('=')) Token(TokenType.LESS_EQUAL, "<=", null, line)
            else Token(TokenType.LESS, "<", null, line)

        '>' ->
            if (match('=')) Token(TokenType.GREATER_EQUAL, ">=", null, line)
            else Token(TokenType.GREATER, ">", null, line)

        '/' ->
            if (match('/')) {
                while (peekIs { it != '\n' }) advance()
                null
            } else Token(TokenType.SLASH, "/", null, line)

        // skip
        ' ', '\r', '\t' -> null

        // new line
        '\n' -> {
            line++
            null
        }

        // string
        '"' -> string()

        // digit, alpha or error
        else -> when {
            c.isDigit() -> number()
            c.isIdentifierStart() -> identifier()
            else -> {
                LErr.error(line, "Unexpected character '${c}'\n")
                null
            }
        }
    }
}


/**
 * 捕获一个基于 start 和 current 指针的、长度可变的 Token
 * 主要针对 string、number、identifier 这种文本可变的 Token
 * @param type token的类型
 * @param literal （可选）字面量的值
 */
private fun ScannerState.captureTokenWithVariableText(type: TokenType, literal: Any? = null): Token {
    val text = source.substring(start, current)  // 这个地方的逻辑几乎是为标识符专门准备的
    return Token(type, text, literal, line)
}

// 消费当前字符
private fun ScannerState.advance(): Char? = source.getOrNull(current++)

// 查看当前字符
private fun ScannerState.peek(): Char? = source.getOrNull(current)

/**
 * 查看并判断当前字符是否符合规则
 * @param predicate lambda 规则
 * @return 是否符合规则
 */
private fun ScannerState.peekIs(predicate: (Char) -> Boolean): Boolean = peek()?.let(predicate) ?: false

// 查看下一个字符
private fun ScannerState.peekNext(): Char? = source.getOrNull(current + 1)

/**
 * 查看并判断下一个字符是否符合规则
 * @param predicate lambda 规则
 * @return 是否符合规则
 */
private fun ScannerState.peekNextIs(predicate: (Char) -> Boolean): Boolean = peekNext()?.let(predicate) ?: false

// 是否到了tokens的末尾
private fun ScannerState.isAtEnd() = current >= source.length

// 匹配当前字符
private fun ScannerState.match(expected: Char): Boolean = when (peek()) {
    expected -> {
        current++
        true
    }

    else -> false
}

// 字符串的匹配规则
private fun ScannerState.string(): Token? {
    while (peekIs { it != '"' }) {
        if (peek() == '\n') line++
        advance()
    }

    // 没匹配到对应的双引号，说明字符串不完整
    if (isAtEnd()) {
        LErr.error(line, "Unterminated string.")
        return null
    }

    // 把终止双引号消费掉
    advance()

    // 提取字符串值
    val value = source.substring(start + 1, current - 1)

    return captureTokenWithVariableText(TokenType.STRING, value)
}

// 数字的匹配规则
private fun ScannerState.number(): Token {
    while (peekIs(Char::isDigit)) advance()

    if (peekIs { it == '.' } && peekNextIs(Char::isDigit)) {
        advance()  // Consume the "."
        while (peekIs(Char::isDigit)) advance()
    }

    return captureTokenWithVariableText(
        TokenType.NUMBER,
        source.substring(start, current).toDouble()
    )
}

// 标识符的匹配规则
private fun ScannerState.identifier(): Token {
    while (peekIs(Char::isIdentifierPart)) advance()

    val text = source.substring(start, current)
    val type = keywords[text] ?: TokenType.IDENTIFIER
    return captureTokenWithVariableText(type)
}


private val keywords = hashMapOf(
    "and" to TokenType.AND,
    "class" to TokenType.CLASS,
    "else" to TokenType.ELSE,
    "false" to TokenType.FALSE,
    "for" to TokenType.FOR,
    "fun" to TokenType.FUN,
    "if" to TokenType.IF,
    "nil" to TokenType.NIL,
    "or" to TokenType.OR,
    "print" to TokenType.PRINT,
    "return" to TokenType.RETURN,
    "super" to TokenType.SUPER,
    "this" to TokenType.THIS,
    "true" to TokenType.TRUE,
    "var" to TokenType.VAR,
    "while" to TokenType.WHILE
)
