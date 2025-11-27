package exp.compiler.klox

import exp.compiler.klox.common.LConfig
import exp.compiler.klox.common.LErr
import exp.compiler.klox.frontend.parse
import exp.compiler.klox.frontend.resolve
import exp.compiler.klox.frontend.scan
import exp.compiler.klox.runtime.Environment
import exp.compiler.klox.runtime.InterpreterState
import exp.compiler.klox.runtime.LCallable
import exp.compiler.klox.runtime.interpret
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) {
    // TODO: 优化参数解析
    LConfig.isDebug = "--debug" in args

    val programArgs = args.filter { it != "--debug" }

    // 根环境
    val rootEnvironment = Environment().apply {
        define("clock", object : LCallable {
            override val arity: Int = 0

            context(iState: InterpreterState)
            override fun call(arguments: List<Any?>): Long = System.currentTimeMillis().milliseconds.inWholeSeconds

            override fun toString(): String = "<native fn clock>"
        })
    }

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
    }.forEach { it.run(globalEnv) }

    LErr.resetError()
}

private fun runFile(path: String, globalEnv: Environment) {
    File(path)
        .readText(Charsets.UTF_8)
        .run(globalEnv)

    if (LErr.hadError) exitProcess(65)
    if (LErr.hadRuntimeError) exitProcess(70)
}


private fun String.run(globalEnv: Environment) = this
    .scan()
    .parse()
    .apply {
        with(InterpreterState(globalEnv, resolve())) {
            if (LErr.hadError) return@apply
            interpret()
        }
    }
