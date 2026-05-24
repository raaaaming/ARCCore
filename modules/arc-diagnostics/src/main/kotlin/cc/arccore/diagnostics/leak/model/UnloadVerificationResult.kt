package cc.arccore.diagnostics.leak.model

data class UnloadVerificationResult(
    val moduleId: String,
    val verifiedAt: Long,
    val cleanupComplete: Boolean,
    val registryClean: Boolean,
    val classLoaderDereferenced: Boolean,
    val activeResourcesClean: Boolean,
    val gcCandidate: Boolean,
    val leaks: List<LeakReport>
) {
    val isClean: Boolean
        get() = cleanupComplete && registryClean && classLoaderDereferenced && activeResourcesClean && leaks.isEmpty()

    val hasPotentialLeak: Boolean
        get() = !gcCandidate || leaks.isNotEmpty()

    val criticalLeakCount: Int
        get() = leaks.count { it.severity == LeakSeverity.CRITICAL }

    val highLeakCount: Int
        get() = leaks.count { it.severity == LeakSeverity.HIGH }
}
