package exp.compiler.klox.lang

/**
 * 词法单元
 * @param TokenType 词法单元类型
 * @param lexeme 词素，即在代码中的模样，比如 LEFT_PAREN 是 "("，变量 myVar 是 "myVar"
 * @param literal 字面量值，存储数字和字符串的具体值
 * @param line Token所在行，报错用
 */
internal data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int
) {
    override fun toString(): String {
        return "$type | $lexeme | $literal"
    }
}
