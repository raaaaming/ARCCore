package cc.arccore.storage.runtime.diagnostics

/**
 * Provides observability into the storage runtime.
 */
interface StorageDiagnostics {

    /**
     * Returns a point-in-time [StorageSnapshot] for the entire runtime.
     */
    fun snapshot(): StorageSnapshot

    /**
     * Returns a [StorageSnapshot] scoped to a single [moduleId].
     */
    fun snapshotForModule(moduleId: String): StorageSnapshot

    /**
     * Returns the total number of open storage handles across all modules.
     */
    fun totalOpenHandles(): Int

    /**
     * Returns the number of open handles for [moduleId].
     */
    fun openHandlesFor(moduleId: String): Int
}
