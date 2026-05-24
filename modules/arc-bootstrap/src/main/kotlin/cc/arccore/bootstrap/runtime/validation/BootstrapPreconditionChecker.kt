package cc.arccore.bootstrap.runtime.validation

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapPhase

/**
 * Checks runtime preconditions that must hold before a bootstrap phase executes.
 * Unlike BootstrapValidator which checks context state, this checks system preconditions
 * (ClassLoader availability, resource accessibility, etc.).
 */
class BootstrapPreconditionChecker {

    fun checkBefore(context: BootstrapContext, phase: BootstrapPhase): ValidationResult {
        return when (phase) {
            BootstrapPhase.DISCOVERY -> checkDiscoveryPreconditions(context)
            BootstrapPhase.METADATA_PRELOAD -> checkMetadataPreloadPreconditions(context)
            BootstrapPhase.DEPENDENCY_GRAPH_BUILD -> checkDependencyGraphPreconditions(context)
            BootstrapPhase.CLASSLOADER_PREPARE -> checkClassLoaderPreparePreconditions(context)
            BootstrapPhase.GENERATED_BOOTSTRAP -> ValidationResult.Ok
            BootstrapPhase.SERVICE_WIRING -> ValidationResult.Ok
            BootstrapPhase.ENABLE_PHASE -> ValidationResult.Ok
            BootstrapPhase.POST_ENABLE -> ValidationResult.Ok
            BootstrapPhase.FAILED, BootstrapPhase.SKIPPED -> ValidationResult.Ok
        }
    }

    private fun checkDiscoveryPreconditions(context: BootstrapContext): ValidationResult {
        if (context.moduleId.isBlank()) {
            return ValidationResult.Fail(
                phase = BootstrapPhase.DISCOVERY,
                reason = "moduleId is blank — cannot discover an unnamed module"
            )
        }
        return ValidationResult.Ok
    }

    private fun checkMetadataPreloadPreconditions(context: BootstrapContext): ValidationResult {
        // ClassLoader must be available
        @Suppress("TooGenericExceptionCaught")
        try {
            context.classLoader.getResources("META-INF/MANIFEST.MF")
        } catch (e: Exception) {
            return ValidationResult.Fail(
                phase = BootstrapPhase.METADATA_PRELOAD,
                reason = "ClassLoader is not functional: ${e.message}",
                cause = e
            )
        }
        return ValidationResult.Ok
    }

    private fun checkDependencyGraphPreconditions(context: BootstrapContext): ValidationResult {
        if (context.moduleId.isBlank()) {
            return ValidationResult.Fail(
                phase = BootstrapPhase.DEPENDENCY_GRAPH_BUILD,
                reason = "moduleId is blank"
            )
        }
        return ValidationResult.Ok
    }

    private fun checkClassLoaderPreparePreconditions(context: BootstrapContext): ValidationResult {
        return ValidationResult.Ok
    }
}
