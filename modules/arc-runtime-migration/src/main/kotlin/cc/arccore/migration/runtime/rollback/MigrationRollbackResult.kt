package cc.arccore.migration.runtime.rollback

data class MigrationRollbackResult(
    val success: Boolean,
    val drainReleased: Boolean = false,
    val ownershipRestoredToSource: Boolean = false,
    val targetBootstrapCleaned: Boolean = false,
    val failedSteps: List<Pair<String, Throwable>> = emptyList()
)
