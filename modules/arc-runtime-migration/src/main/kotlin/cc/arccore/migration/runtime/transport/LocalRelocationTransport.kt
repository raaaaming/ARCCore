package cc.arccore.migration.runtime.transport

import java.util.concurrent.ConcurrentHashMap

class LocalRelocationTransport : RuntimeRelocationTransport {
    override val transportId: String = "local"

    private val snapshotStore = ConcurrentHashMap<String, ByteArray>()
    private val bootstrapTriggers = ConcurrentHashMap<String, Boolean>()
    private val routingSwitches = ConcurrentHashMap<String, Long>()

    private val pollingSleepMs: Long = 10L

    override fun canReach(targetNodeId: String): Boolean = targetNodeId == "local"

    override fun ping(targetNodeId: String): TransportPingResult {
        return if (targetNodeId == "local") {
            TransportPingResult.Reachable(latencyMs = 0L)
        } else {
            TransportPingResult.Unreachable(reason = "Node '$targetNodeId' is not reachable via local transport")
        }
    }

    override fun transferSnapshot(
        targetNodeId: String,
        snapshotData: ByteArray,
        moduleId: String,
        snapshotId: String
    ): TransportTransferResult {
        val startMs = System.currentTimeMillis()
        return try {
            val key = snapshotKey(moduleId, snapshotId)
            snapshotStore[key] = snapshotData
            val durationMs = System.currentTimeMillis() - startMs
            TransportTransferResult.Success(
                transferredBytes = snapshotData.size.toLong(),
                durationMs = durationMs
            )
        } catch (e: Exception) {
            TransportTransferResult.Failure(error = e)
        }
    }

    override fun triggerBootstrap(
        targetNodeId: String,
        moduleId: String,
        snapshotId: String
    ): TransportBootstrapResult {
        return try {
            bootstrapTriggers[bootstrapKey(moduleId, snapshotId)] = true
            TransportBootstrapResult.Success
        } catch (e: Exception) {
            TransportBootstrapResult.Failure(error = e)
        }
    }

    override fun awaitRestoreReady(
        targetNodeId: String,
        moduleId: String,
        timeoutMs: Long
    ): TransportReadinessResult {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val triggered = bootstrapTriggers.values.any { it }
            if (triggered) return TransportReadinessResult.Ready
            Thread.sleep(pollingSleepMs)
        }
        return TransportReadinessResult.TimedOut
    }

    override fun notifyRoutingSwitch(targetNodeId: String, moduleId: String): TransportAckResult {
        return try {
            routingSwitches[moduleId] = System.currentTimeMillis()
            TransportAckResult.Acknowledged
        } catch (e: Exception) {
            TransportAckResult.Failed(error = e)
        }
    }

    fun getStoredSnapshot(moduleId: String, snapshotId: String): ByteArray? {
        return snapshotStore[snapshotKey(moduleId, snapshotId)]
    }

    private fun snapshotKey(moduleId: String, snapshotId: String): String = "$moduleId::$snapshotId"

    private fun bootstrapKey(moduleId: String, snapshotId: String): String = "$moduleId::$snapshotId"
}
