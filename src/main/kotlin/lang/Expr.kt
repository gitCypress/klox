package exp.compiler.klox.lang

// Auto-generated code, DO NOT modify directly.

internal sealed class Expr {
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    
    data class Grouping(val expression: Expr) : Expr()
    
    data class Literal(val value: Any?) : Expr()
    
    data class Unary(val operator: Token, val right: Expr) : Expr()
}