package cc.arccore.bootstrap.runtime.validation

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapContextKey
import cc.arccore.bootstrap.runtime.BootstrapPhase

/**
 * Validates BootstrapContext state before and after each phase.
 * Pre-condition: required slots are present in context.
 * Post-condition: the phase produced expected output slots.
 */
class BootstrapValidator {

    fun validatePreCondition(context: BootstrapContext, phase: BootstrapPhase): ValidationResult =
        when (phase) {
            BootstrapPhase.DISCOVERY -> {
                if (context.moduleId.isBlank())
                    ValidationResult.Fail(phase, "moduleId must not be blank before DISCOVERY")
                else ValidationResult.Ok
            }
            BootstrapPhase.METADATA_PRELOAD -> ValidationResult.Ok
            BootstrapPhase.DEPENDENCY_GRAPH_BUILD -> {
                val preloaded = context.get(BootstrapContextKey.PRELOADED_METADATA)
                if (preloaded == null)
                    ValidationResult.Fail(
                        phase,
                        "PRELOADED_METADATA slot is missing — METADATA_PRELOAD must run first"
                    )
                else ValidationResult.Ok
            }
            BootstrapPhase.CLASSLOADER_PREPARE -> ValidationResult.Ok
            BootstrapPhase.GENERATED_BOOTSTRAP -> {
                val depOrder = context.get(BootstrapContextKey.DEPENDENCY_ORDER)
                if (depOrder == null)
                    ValidationResult.Fail(
                        phase,
                        "DEPENDENCY_ORDER slot is missing — DEPENDENCY_GRAPH_BUILD must run first"
                    )
                else ValidationResult.Ok
            }
            BootstrapPhase.SERVICE_WIRING -> {
                val registrarResult = context.get(BootstrapContextKey.REGISTRAR_RESULT)
                if (registrarResult == null)
                    ValidationResult.Fail(
                        phase,
                        "REGISTRAR_RESULT slot is missing — GENERATED_BOOTSTRAP must run first"
                    )
                else ValidationResult.Ok
            }
            BootstrapPhase.ENABLE_PHASE -> ValidationResult.Ok
            BootstrapPhase.POST_ENABLE -> ValidationResult.Ok
            BootstrapPhase.FAILED, BootstrapPhase.SKIPPED -> ValidationResult.Ok
        }

    fun validatePostCondition(context: BootstrapContext, phase: BootstrapPhase): ValidationResult =
        when (phase) {
            BootstrapPhase.METADATA_PRELOAD -> {
                val preloaded = context.get(BootstrapContextKey.PRELOADED_METADATA)
                if (preloaded == null)
                    ValidationResult.Fail(
                        phase,
                        "METADATA_PRELOAD completed but PRELOADED_METADATA slot not populated"
                    )
                else ValidationResult.Ok
            }
            BootstrapPhase.DEPENDENCY_GRAPH_BUILD -> {
                val depOrder = context.get(BootstrapContextKey.DEPENDENCY_ORDER)
                if (depOrder == null)
                    ValidationResult.Fail(
                        phase,
                        "DEPENDENCY_GRAPH_BUILD completed but DEPENDENCY_ORDER slot not populated"
                    )
                else ValidationResult.Ok
            }
            BootstrapPhase.GENERATED_BOOTSTRAP -> {
                // Only require REGISTRAR_RESULT when generated artifacts were actually present.
                // If PRELOADED_METADATA indicates no artifacts, REGISTRAR_RESULT is not expected.
                val preloaded = context.get(BootstrapContextKey.PRELOADED_METADATA)
                val hasArtifacts = preloaded?.artifactManifest?.isPartiallyGenerated ?: false
                if (hasArtifacts) {
                    val registrarResult = context.get(BootstrapContextKey.REGISTRAR_RESULT)
                    if (registrarResult == null)
                        ValidationResult.Fail(
                            phase,
                            "GENERATED_BOOTSTRAP completed but REGISTRAR_RESULT slot not populated"
                        )
                    else ValidationResult.Ok
                } else {
                    ValidationResult.Ok
                }
            }
            else -> ValidationResult.Ok
        }
}
