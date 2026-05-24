package cc.arccore.bootstrap.runtime.validation

import cc.arccore.bootstrap.runtime.BootstrapPhase

sealed class ValidationResult {

    data object Ok : ValidationResult()

    data class Fail(
        val phase: BootstrapPhase,
        val reason: String,
        val cause: Throwable? = null
    ) : ValidationResult()

    val isOk: Boolean get() = this is Ok
    val isFail: Boolean get() = this is Fail
}
