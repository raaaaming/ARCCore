package cc.arccore.runtime.context.validation

import cc.arccore.api.module.ModuleState
import cc.arccore.runtime.context.RuntimeModuleContext
import cc.arccore.runtime.context.exception.InvalidModuleContextException

object ContextValidator {

    fun validate(context: RuntimeModuleContext): ValidationResult {
        val violations = mutableListOf<String>()

        if (context.state == ModuleState.UNLOADED && !context.cleanupScope.isClosed) {
            violations.add("Module is UNLOADED but cleanupScope is still open")
        }

        if (context.description.id.isBlank()) {
            violations.add("Module description.id must not be blank")
        }

        if (context.cleanupScope.isClosed && context.state == ModuleState.ENABLED) {
            violations.add("cleanupScope is closed but module state is ENABLED — state/scope mismatch")
        }

        return ValidationResult(violations.isEmpty(), violations)
    }

    fun validateOrThrow(context: RuntimeModuleContext) {
        val result = validate(context)
        if (!result.valid) {
            throw InvalidModuleContextException(
                "Module context validation failed for '${context.description.id}': ${result.violations.joinToString("; ")}"
            )
        }
    }

    data class ValidationResult(val valid: Boolean, val violations: List<String>)
}
