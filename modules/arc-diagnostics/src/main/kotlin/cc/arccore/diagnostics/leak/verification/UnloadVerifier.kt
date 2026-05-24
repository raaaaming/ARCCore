package cc.arccore.diagnostics.leak.verification

import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakSeverity
import cc.arccore.diagnostics.leak.model.LeakType
import cc.arccore.diagnostics.leak.model.UnloadVerificationResult
import cc.arccore.diagnostics.leak.resource.OrphanResourceDetector
import cc.arccore.diagnostics.leak.tracking.LeakRecordRegistry
import cc.arccore.diagnostics.leak.weakref.ClassLoaderLeakTracker

/**
 * Verifies that a module was fully unloaded and is safe to be GC'd.
 *
 * Unload verification sequence:
 *   1. (Optional) Hint GC with System.gc() — not guaranteed to run, but increases confidence
 *   2. Drain the ReferenceQueue to update GC candidate status
 *   3. Check ClassLoader WeakReference — if still reachable, something holds a strong ref
 *   4. Check OrphanResourceDetector for lingering executors or scheduler tasks
 *   5. Check LeakRecordRegistry for any previously recorded leaks for this module
 *   6. Build UnloadVerificationResult
 *
 * Note on System.gc():
 *   System.gc() is a hint, not a guarantee. The JVM may ignore it (e.g., G1GC with -XX:+DisableExplicitGC).
 *   A cleared WeakReference confirms GC-safety; an uncleared one only means "not yet collected,"
 *   not definitively "leaked." Use triggerGcHint=true only in diagnostic/admin contexts, never in hot paths.
 */
class UnloadVerifier(
    private val classLoaderTracker: ClassLoaderLeakTracker,
    private val orphanDetector: OrphanResourceDetector,
    private val leakRegistry: LeakRecordRegistry
) {

    fun verify(moduleId: String, triggerGcHint: Boolean = false): UnloadVerificationResult {
        if (triggerGcHint) {
            System.gc()
            // Brief pause to let the GC run before draining the ReferenceQueue.
            // This is intentional in admin/diagnostic paths only.
            try { Thread.sleep(100) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }

        classLoaderTracker.drainCollected()

        val gcCandidate = classLoaderTracker.isGcCandidate(moduleId)
        val orphanLeaks = orphanDetector.detect(moduleId)
        val recordedLeaks = leakRegistry.getForModule(moduleId)

        val classLoaderLeak = if (!gcCandidate) {
            LeakReport(
                moduleId = moduleId,
                type = LeakType.CLASSLOADER_LEAK,
                severity = LeakSeverity.CRITICAL,
                description = "ClassLoader for module '$moduleId' is still reachable after unload. " +
                    "A strong reference somewhere in the JVM is preventing GC — classloader leak confirmed.",
                details = mapOf("gcCandidate" to "false")
            )
        } else null

        val allLeaks = buildList {
            if (classLoaderLeak != null) add(classLoaderLeak)
            addAll(orphanLeaks)
            addAll(recordedLeaks)
        }

        val hasOrphans = orphanLeaks.isNotEmpty()
        val hasDanglingRegistry = recordedLeaks.any { it.type == LeakType.DANGLING_REGISTRY_ENTRY }
        val hasOrphanResources = recordedLeaks.any {
            it.type in setOf(LeakType.ORPHAN_EXECUTOR, LeakType.ORPHAN_COROUTINE, LeakType.ORPHAN_SCHEDULER_TASK)
        } || hasOrphans

        return UnloadVerificationResult(
            moduleId = moduleId,
            verifiedAt = System.currentTimeMillis(),
            cleanupComplete = !hasOrphans && recordedLeaks.none { it.type == LeakType.ORPHAN_EXECUTOR },
            registryClean = !hasDanglingRegistry,
            classLoaderDereferenced = gcCandidate,
            activeResourcesClean = !hasOrphanResources,
            gcCandidate = gcCandidate,
            leaks = allLeaks
        )
    }
}
