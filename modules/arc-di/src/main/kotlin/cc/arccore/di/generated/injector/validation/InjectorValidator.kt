package cc.arccore.di.generated.injector.validation

import cc.arccore.api.di.generated.GeneratedInjector

/**
 * Runtime validator run once per injector at load time.
 *
 * Catches stale generated artifacts that can appear during incremental builds
 * when only some factory classes were regenerated. The validator performs
 * lightweight checks — all deep structural validation happens at KSP time.
 */
class InjectorValidator {

    companion object {
        const val CURRENT_METADATA_VERSION = 1
    }

    fun validate(injector: GeneratedInjector<*>, classLoader: ClassLoader): ValidationResult {
        val targetName = injector.targetClass.java.name

        // Verify the target class is still reachable from this classloader.
        // A mismatch here means the factory was generated against a class that
        // has since been removed or renamed — fail fast instead of a confusing NPE.
        try {
            classLoader.loadClass(targetName)
        } catch (_: ClassNotFoundException) {
            return ValidationResult.Invalid(
                "Target class '$targetName' not found — stale generated injector. " +
                    "Rebuild the module to regenerate factories."
            )
        }

        if (injector.metadataVersion != CURRENT_METADATA_VERSION) {
            return ValidationResult.Invalid(
                "Metadata version mismatch for '$targetName': " +
                    "expected $CURRENT_METADATA_VERSION, got ${injector.metadataVersion}. " +
                    "Rebuild the module."
            )
        }

        return ValidationResult.Valid
    }

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
