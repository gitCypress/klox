package exp.compiler.klox.lang

// Auto-generated code, DO NOT modify directly.

internal sealed class Stmt {
    data class Block(val statements: List<Stmt>) : Stmt()
    
    data class Expression(val expression: Expr) : Stmt()
    
    data class Print(val expression: Expr) : Stmt()
    
    data class Var(val name: Token, val initializer: Expr?) : Stmt()
}