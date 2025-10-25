package exp.compiler.klox.common

import exp.compiler.klox.lang.Token
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal fun Any?.stringify(): String = when (this) {
    null -> "nil"
    is Double -> this.toString().removeSuffix(".0")
    else -> this.toString()
}

@OptIn(ExperimentalContracts::class)
internal inline fun loxRequire(condition: Boolean, token: Token, message: () -> String) {
    // 签订契约：如果函数成功返回，那么条件 condition 必然为 true
    contract {
        returns() implies condition
    }
    if (!condition) {
        // ✅ 抛出我们自己的 RuntimeError
        throw RuntimeError(token.line, message())
    }
}
