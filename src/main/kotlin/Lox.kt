package exp.compiler.klox

import exp.compiler.klox.common.*
import exp.compiler.klox.fronted.interpret
import exp.compiler.klox.tools.toAST
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // TODO: 优化参数解析
    LConfig.isDebug = "--debug" in args

    val programArgs = args.filter { it != "--debug" }
    when {
        programArgs.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }

        programArgs.size == 1 -> runFile(args[0])
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
    if (LErr.hadRuntimeError) exitProcess(70)
}

private fun run(source: String) = source
    .scanTokens().also { tprintln("[main::run/scanTokens] $it") }
    .parse().also { tprintln("[main::run/parse(toAST)] ${it?.toAST()}") }
    ?.interpret()
    .also { println(it.stringify()) }



