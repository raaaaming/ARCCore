package cc.arccore.diagnostics.model

import cc.arccore.api.module.ModuleState

data class ModuleSnapshot(
    val moduleId: String,
    val moduleName: String,
    val version: String,
    val state: ModuleState,
    val loadedAt: Long?,           // epoch millis, null if not tracked yet
    val enabledAt: Long?,          // epoch millis
    val reloadGeneration: Int,     // 0 = initial load, 1+ = reload count
    val classLoaderId: String?,    // System.identityHashCode hex of classLoader
    val activeServices: Set<String>,   // KClass.simpleName 또는 qualifiedName
    val activeCommands: Set<String>,   // command names
    val activeListeners: Set<String>,  // listener class simple names
    val activeAsyncTasks: Int,         // coroutine/async job count
    val dependencies: List<String>,    // declared dependency IDs
    val failureCause: String?          // failureCause?.message, null if healthy
)
