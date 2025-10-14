package exp.compiler.klox

import exp.compiler.klox.common.*
import exp.compiler.klox.tools.toAST
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    when {
        args.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }

        args.size == 1 -> runFile(args[0])
        else -> runPrompt()
    }
}

private fun runPrompt() {
    generateSequence {
        print("> ")
        readlnOrNull()
    }.forEach { line ->
        run(line)
    }

    LErr.resetError()
}

private fun runFile(path: String) {
    val sourceCode = File(path).readText()
    run(sourceCode)

    if (LErr.hadError) exitProcess(65)
}

private fun run(source: String) = source
    .scanTokens().also { println("[test:main::run/scanTokens] $it") }
    .parse()
    ?.toAST()
    ?.let { println(it) }



