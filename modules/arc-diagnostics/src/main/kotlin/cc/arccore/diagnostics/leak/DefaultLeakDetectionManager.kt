package cc.arccore.diagnostics.leak

import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.diagnostics.leak.integrity.RuntimeIntegrityValidator
import cc.arccore.diagnostics.leak.lifecycle.LeakDetectionLifecycleObserver
import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.UnloadVerificationResult
import cc.arccore.diagnostics.leak.reporting.LeakReporter
import cc.arccore.diagnostics.leak.resource.OrphanResourceDetector
import cc.arccore.diagnostics.leak.tracking.LeakRecordRegistry
import cc.arccore.diagnostics.leak.verification.UnloadVerifier
import cc.arccore.diagnostics.leak.weakref.ClassLoaderLeakTracker
import cc.arccore.runtime.lifecycle.ModuleRuntime

class DefaultLeakDetectionManager(
    private val runtime: ModuleRuntime
) : LeakDetectionManager {

    private val classLoaderTracker = ClassLoaderLeakTracker()
    private val leakRegistry = LeakRecordRegistry()
    private val orphanDetector = OrphanResourceDetector()
    private val reporter = LeakReporter()

    private val unloadVerifier = UnloadVerifier(
        classLoaderTracker = classLoaderTracker,
        orphanDetector = orphanDetector,
        leakRegistry = leakRegistry
    )

    private val integrityValidator = RuntimeIntegrityValidator(
        runtime = runtime,
        classLoaderTracker = classLoaderTracker
    )

    override val lifecycleObserver: LifecycleObserver = LeakDetectionLifecycleObserver(
        classLoaderTracker = classLoaderTracker,
        onUnloadedCallback = { moduleId ->
            // Cleanup orphan resource tracking after unload.
            // Verification is left to explicit calls (/arc leaks verify <id>).
            orphanDetector.cleanup(moduleId)
        }
    )

    override fun verifyUnload(moduleId: String, gcHint: Boolean): UnloadVerificationResult =
        unloadVerifier.verify(moduleId, gcHint)

    override fun getAllLeaks(): List<LeakReport> = leakRegistry.getAll()

    override fun getLeaksForModule(moduleId: String): List<LeakReport> =
        leakRegistry.getForModule(moduleId)

    override fun validateIntegrity(): List<LeakReport> = integrityValidator.validate()

    override fun recordLeak(report: LeakReport) {
        leakRegistry.record(report)
    }

    override fun clearModuleLeaks(moduleId: String) {
        leakRegistry.clearModule(moduleId)
        classLoaderTracker.remove(moduleId)
        orphanDetector.cleanup(moduleId)
    }

    override fun getReporter(): LeakReporter = reporter

    override fun shutdown() {
        runtime.removeLifecycleObserver(lifecycleObserver)
        leakRegistry.clear()
        orphanDetector.cleanupAll()
    }
}
