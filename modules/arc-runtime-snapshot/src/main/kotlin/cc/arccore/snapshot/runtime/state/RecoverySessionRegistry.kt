package cc.arccore.snapshot.runtime.state

import java.util.concurrent.ConcurrentHashMap

internal class RecoverySessionRegistry {
    private val activeSessions = ConcurrentHashMap.newKeySet<String>()

    fun begin(runtimeId: String): Boolean = activeSessions.add(runtimeId)

    fun complete(runtimeId: String) = activeSessions.remove(runtimeId)

    fun isRecovering(runtimeId: String): Boolean = activeSessions.contains(runtimeId)

    fun allActive(): Set<String> = activeSessions.toSet()
}
