package cc.arccore.storage.runtime.cleanup

/**
 * A single, discrete step in the storage cleanup pipeline.
 *
 * This interface is deliberately free of arc-runtime dependencies so that
 * arc-storage can implement cleanup steps without an upward cycle.
 * The arc-runtime unload pipeline bridges these steps via an adapter.
 */
interface StorageCleanupStep {

    /**
     * A human-readable name for this step, used in logging and diagnostics.
     */
    val stepName: String

    /**
     * Executes this cleanup step for [moduleId].
     *
     * @return A [StorageCleanupResult] describing the outcome.
     */
    fun execute(moduleId: String): StorageCleanupResult
}
