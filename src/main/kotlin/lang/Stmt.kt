package exp.compiler.klox.lang

// Auto-generated code, DO NOT modify directly.

internal sealed class Stmt {
    data class Expression(val expression: Expr) : Stmt()
    
    data class Print(val expression: Expr) : Stmt()
}