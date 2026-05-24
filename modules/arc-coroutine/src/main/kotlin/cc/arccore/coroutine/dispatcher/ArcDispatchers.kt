package cc.arccore.coroutine.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.bukkit.plugin.Plugin

class ArcDispatchers(plugin: Plugin) {
    val sync: CoroutineDispatcher = BukkitSyncDispatcher(plugin)
    val async: CoroutineDispatcher = Dispatchers.Default
    val io: CoroutineDispatcher = Dispatchers.IO
    // Tick: 현재는 sync와 동일 (미래 per-tick dispatcher 확장 가능)
    val tick: CoroutineDispatcher = sync

    fun forDispatcher(dispatcher: ModuleDispatcher): CoroutineDispatcher = when (dispatcher) {
        is ModuleDispatcher.Sync -> sync
        is ModuleDispatcher.Async -> async
        is ModuleDispatcher.IO -> io
        is ModuleDispatcher.Tick -> tick
    }
}
