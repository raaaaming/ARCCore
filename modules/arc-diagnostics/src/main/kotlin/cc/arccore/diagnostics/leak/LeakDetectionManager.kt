package cc.arccore.diagnostics.leak

import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.UnloadVerificationResult
import cc.arccore.diagnostics.leak.reporting.LeakReporter

interface LeakDetectionManager {

    /** Observer to register with ModuleRuntime for lifecycle-driven classloader capture. */
    val lifecycleObserver: LifecycleObserver

    /**
     * Verifies whether a module was cleanly unloaded and its ClassLoader is GC-eligible.
     * Pass [gcHint] = true to call System.gc() before checking (admin/diagnostic paths only).
     */
    fun verifyUnload(moduleId: String, gcHint: Boolean = false): UnloadVerificationResult

    /** Returns all leak reports recorded across all modules. */
    fun getAllLeaks(): List<LeakReport>

    /** Returns all leak reports recorded for a specific module. */
    fun getLeaksForModule(moduleId: String): List<LeakReport>

    /**
     * Validates runtime-wide integrity: orphan states, stale registry entries,
     * and classloader leaks across unloaded modules.
     */
    fun validateIntegrity(): List<LeakReport>

    /**
     * Records a leak report externally (e.g., from module infrastructure detecting a stale ref).
     */
    fun recordLeak(report: LeakReport)

    /** Clears all recorded leaks for a module (e.g., after confirmed cleanup). */
    fun clearModuleLeaks(moduleId: String)

    fun getReporter(): LeakReporter

    fun shutdown()
}
