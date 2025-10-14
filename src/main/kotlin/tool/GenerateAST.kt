// GenerateAST.kt (最终“更 Kotlin”的版本)

package exp.compiler.klox.tool

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }

    val outputDir: String = args[0]
    val baseName = "Expr"

    val path = "$outputDir/$baseName.kt"

    val content = defineAst(
        baseName, listOf(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Unary    : Token operator, Expr right"
        )
    )

    File(path).writeText(content, Charsets.UTF_8)
}

/**
 * 组装整个 AST 文件，现在不再包含 Visitor 相关代码。
 */
private fun defineAst(baseName: String, types: List<String>): String {
    val typeClasses = types.joinToString("\n\n") { type ->
        val (className, fields) = type
            .split(':')
            .map(String::trim)
        defineType(baseName, className, fields)
    }

    return """
        |package exp.compiler.klox
        |
        |// Auto-generated code, DO NOT modify directly.
        |
        |internal sealed class $baseName {
        |${typeClasses.prependIndent("    ")}
        |}
    """.trimMargin()
}

/**
 * 为单个 AST 类型生成一个干净的 data class。
 */
private fun defineType(
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