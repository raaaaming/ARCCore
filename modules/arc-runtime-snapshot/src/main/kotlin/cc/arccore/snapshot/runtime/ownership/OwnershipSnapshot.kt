package cc.arccore.snapshot.runtime.ownership

import cc.arccore.snapshot.runtime.model.SnapshotId
import java.time.Instant

data class OwnershipSnapshot(
    val snapshotId: SnapshotId,
    val runtimeId: String,
    val capturedAt: Instant = Instant.now(),
    val schedulerOwnership: Map<String, Any?> = emptyMap(),
    val actorOwnership: Map<String, Any?> = emptyMap(),
    val subscriptionOwnership: Map<String, Any?> = emptyMap(),
    val distributedOwnership: Map<String, Any?> = emptyMap()
) {
    fun isEmpty(): Boolean = schedulerOwnership.isEmpty() &&
            actorOwnership.isEmpty() &&
            subscriptionOwnership.isEmpty() &&
            distributedOwnership.isEmpty()

    fun toStateMap(): Map<String, Any?> = mapOf(
        "scheduler" to schedulerOwnership,
        "actor" to actorOwnership,
        "subscription" to subscriptionOwnership,
        "distributed" to distributedOwnership
    )
}
