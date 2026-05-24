package cc.arccore.snapshot.runtime.snapshot

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot

interface SnapshotableRuntime {
    val runtimeId: String
    val runtimeType: String get() = "generic"

    fun captureSnapshot(): RuntimeSnapshot

    fun supportsOwnershipSnapshot(): Boolean = false

    fun captureOwnershipSnapshot(): Map<String, Any?> = emptyMap()

    // 미래 확장 포인트: incremental snapshot
    fun supportsIncrementalSnapshot(): Boolean = false
    fun captureIncrementalSnapshot(basedOn: RuntimeSnapshot): RuntimeSnapshot = captureSnapshot()

    // 미래 확장 포인트: distributed snapshot
    fun supportsDistributedSnapshot(): Boolean = false
}
