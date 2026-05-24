package cc.arccore.runtime.reload.validation

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.reload.ModuleReloadHint
import java.nio.file.Path

object ReloadValidator {

    sealed class ValidationFailure {
        data class InvalidState(val moduleId: String, val actual: ModuleState) : ValidationFailure()
        data class JarNotFound(val moduleId: String, val path: Path) : ValidationFailure()
        data class ConditionalRejected(val moduleId: String, val reason: String) : ValidationFailure()
    }

    fun validate(container: ModuleContainerView, jarPath: Path): ValidationFailure? {
        val validStates = setOf(ModuleState.ENABLED, ModuleState.DISABLED)
        if (container.state !in validStates) {
            return ValidationFailure.InvalidState(container.module.id, container.state)
        }
        if (!jarPath.toFile().exists()) {
            return ValidationFailure.JarNotFound(container.module.id, jarPath)
        }
        val module = container.module
        if (module is ModuleReloadHint.ConditionalReload && !module.canReload()) {
            return ValidationFailure.ConditionalRejected(container.module.id, module.rejectReason())
        }
        return null
    }
}
