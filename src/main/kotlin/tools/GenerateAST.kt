// GenerateAST.kt (最终“更 Kotlin”的版本)

package exp.compiler.klox.tools

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir: String = args[0]

    // Expr.kt
    generate(
        "$outputDir/Expr.kt", "Expr",
        listOf(
            "Assign   : Token name, Expr value",
            "Binary   : Expr left, Token operator, Expr right",
            "Call     : Expr callee, Token paren, List<Expr> arguments",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Logical  : Expr left, Token logicOp, Expr right",
            "Unary    : Token operator, Expr right",
            "Variable : Token name",
        )
    )

    // Stmt.kt
    generate(
        "$outputDir/Stmt.kt", "Stmt",
        listOf(
            "Block      : List<Stmt> statements",
            "Expression : Expr expression",
            "Function   : Token name, List<Token> params, List<Stmt> body",
            "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
            "Print      : Expr expression",
            "Return     : Token keyword, Expr? value",
            "Var        : Token name, Expr? initializer",
            "While      : Expr condition, Stmt body",
        )
    )
}

/**
 * 组装整个 AST 文件，现在不再包含 Visitor 相关代码。
 */
private fun generate(path: String, baseName: String, types: List<String>) {
    val typeClasses = types.joinToString("\n\n") { type ->
        val (className, fields) = type
            .split(':')
            .map(String::trim)
        genType(baseName, className, fields)
    }

    val content = """
        |package exp.compiler.klox.lang
        |
        |// Auto-generated code, DO NOT modify directly.
        |
        |internal sealed class $baseName {
        |${typeClasses.prependIndent("    ")}
        |}
    """.trimMargin()

    File(path).writeText(content, Charsets.UTF_8)
}

/**
 * 为单个 AST 类型生成一个干净的 data class。
 */
private fun genType(
    baseName: String, className: String, fieldList: String
): String {
    val kotlinFields = fieldList
        .split(", ")
        .filter { it.isNotBlank() }
        .joinToString(", ") { field ->
            val (type, name) = field.split(" ")
            "val $name: $type"
        }

    // 只生成 data class，不再有 accept 方法
    return "data class $className($kotlinFields) : $baseName()"
}