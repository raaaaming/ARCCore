package cc.arccore.diagnostics.leak.validation

import cc.arccore.diagnostics.leak.exception.LeakDetectionException
import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakType

/**
 * Validates inputs and state for leak detection operations.
 * Guards against duplicate tracking, invalid module IDs, and corrupted detection state.
 */
class LeakDetectionValidator {

    fun requireModuleId(moduleId: String) {
        if (moduleId.isBlank()) throw LeakDetectionException("Module ID must not be blank")
    }

    fun requireNonNullClassLoader(moduleId: String, classLoader: ClassLoader?) {
        if (classLoader == null) throw LeakDetectionException(
            "Cannot track classloader for '$moduleId': classLoader is null. " +
                "Ensure the module context is still alive when tracking begins."
        )
    }

    fun warnDuplicateTracking(moduleId: String, alreadyTracked: Boolean): Boolean {
        // Returns true if tracking should proceed (overwrite previous).
        // Duplicate tracking on reload is expected — the old WeakReference is replaced by the new one.
        return true
    }

    fun validateLeakReport(report: LeakReport) {
        requireModuleId(report.moduleId)
        if (report.description.isBlank()) throw LeakDetectionException(
            "LeakReport for '${report.moduleId}' has blank description"
        )
    }

    fun checkForStaleWeakRef(moduleId: String, isGcCandidate: Boolean): LeakReport? {
        return if (!isGcCandidate) {
            LeakReport(
                moduleId = moduleId,
                type = LeakType.CLASSLOADER_LEAK,
                severity = cc.arccore.diagnostics.leak.model.LeakSeverity.CRITICAL,
                description = "Stale WeakReference detected for '$moduleId': classloader not GC'd"
            )
        } else null
    }
}
