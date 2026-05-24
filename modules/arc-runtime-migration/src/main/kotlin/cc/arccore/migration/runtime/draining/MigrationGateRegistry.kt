package cc.arccore.migration.runtime.draining

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class MigrationGateRegistry {
    private val gates = ConcurrentHashMap<String, AtomicBoolean>()

    fun getOrCreate(moduleId: String): AtomicBoolean =
        gates.computeIfAbsent(moduleId) { AtomicBoolean(true) }

    fun remove(moduleId: String): AtomicBoolean? = gates.remove(moduleId)

    fun allGates(): Map<String, AtomicBoolean> = gates.toMap()
}
