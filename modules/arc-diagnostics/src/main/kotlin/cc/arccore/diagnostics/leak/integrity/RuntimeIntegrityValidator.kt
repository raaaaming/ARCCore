package cc.arccore.diagnostics.leak.integrity

import cc.arccore.api.module.ModuleState
import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakSeverity
import cc.arccore.diagnostics.leak.model.LeakType
import cc.arccore.diagnostics.leak.weakref.ClassLoaderLeakTracker
import cc.arccore.runtime.lifecycle.ModuleRuntime

/**
 * Validates the runtime's overall integrity — checks for orphan state, stale registry entries,
 * and classloader leaks across all currently tracked modules.
 *
 * Why runtime integrity validation matters:
 *   After repeated hot-reloads, the runtime can accumulate "phantom" module state:
 *   containers stuck in FAILED without proper cleanup, classloaders from previous generations
 *   still referenced, or leaked coroutine scopes keeping old module instances alive.
 *   Runtime integrity validation provides a single health-check pass over the entire module graph.
 */
class RuntimeIntegrityValidator(
    private val runtime: ModuleRuntime,
    private val classLoaderTracker: ClassLoaderLeakTracker
) {

    fun validate(): List<LeakReport> {
        val reports = mutableListOf<LeakReport>()

        classLoaderTracker.drainCollected()

        for (container in runtime.getContainers()) {
            val moduleId = container.module.id

            if (container.state == ModuleState.FAILED && container.failureCause == null) {
                reports += LeakReport(
                    moduleId = moduleId,
                    type = LeakType.INVALID_LIFECYCLE_STATE,
                    severity = LeakSeverity.MEDIUM,
                    description = "Module '$moduleId' is in FAILED state but has no failure cause — orphan runtime state",
                    details = mapOf("state" to container.state.name)
                )
            }

            // Detect modules that should be UNLOADED but still appear in the container list
            // with a live context (UNLOADED + non-null context = state machine inconsistency)
            if (container.state == ModuleState.UNLOADED && container.context != null) {
                reports += LeakReport(
                    moduleId = moduleId,
                    type = LeakType.DANGLING_REGISTRY_ENTRY,
                    severity = LeakSeverity.HIGH,
                    description = "Module '$moduleId' is UNLOADED but its context is still set — dangling registry entry",
                    details = mapOf("state" to container.state.name, "contextNull" to "false")
                )
            }
        }

        // Check for classloader leaks across all unloaded modules (still tracked but not GC'd)
        val leaking = classLoaderTracker.leakingModules()
        val activeIds = runtime.getContainers().map { it.module.id }.toSet()
        for (moduleId in leaking) {
            if (moduleId !in activeIds) {
                reports += LeakReport(
                    moduleId = moduleId,
                    type = LeakType.CLASSLOADER_LEAK,
                    severity = LeakSeverity.CRITICAL,
                    description = "ClassLoader for unloaded module '$moduleId' is still reachable (WeakReference not cleared). " +
                        "Something is holding a strong reference — check for stale services, listeners, or coroutine captures.",
                    details = mapOf("gcCandidate" to "false", "activeModule" to "false")
                )
            }
        }

        return reports
    }
}
