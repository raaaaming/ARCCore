package cc.arccore.migration.runtime.synchronization

import java.util.concurrent.ConcurrentHashMap

internal class MigrationTransitionLock {
    private val locks = ConcurrentHashMap<String, Boolean>()

    fun tryLock(moduleId: String): Boolean = locks.putIfAbsent(moduleId, true) == null

    fun unlock(moduleId: String) {
        locks.remove(moduleId)
    }

    fun isLocked(moduleId: String): Boolean = locks.containsKey(moduleId)

    fun lockedModules(): Set<String> = locks.keys.toSet()
}
