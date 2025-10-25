package exp.compiler.klox

import exp.compiler.klox.common.*
import exp.compiler.klox.fronted.*
import exp.compiler.klox.frontend.parse
import exp.compiler.klox.runtime.Environment
import exp.compiler.klox.runtime.interpret
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // TODO: 优化参数解析
    LConfig.isDebug = "--debug" in args

    val programArgs = args.filter { it != "--debug" }

    // 根环境
    val rootEnvironment = Environment()

    when {
        programArgs.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }

        programArgs.size == 1 -> runFile(args[0], rootEnvironment)
        else -> runPrompt(rootEnvironment)
    }
}

private fun runPrompt(globalEnv: Environment) {
    generateSequence {
        print("> ")
        readlnOrNull()
    }.forEach { line ->
        run(line, globalEnv)
    }

    LErr.resetError()
}

private fun runFile(path: String, globalEnv: Environment) {
    val sourceCode = File(path).readText()
    run(sourceCode, globalEnv)

    if (LErr.hadError) exitProcess(65)
    if (LErr.hadRuntimeError) exitProcess(70)
}

private fun run(source: String, globalEnv: Environment) = source
    .scan()
//    .also { tprintln("[main::run/scanTokens] $it") }
    .parse()
//    .also { tprintln("[main::run/parse(toAST)] ${it?.toAST()}") }
    .interpret(globalEnv)



