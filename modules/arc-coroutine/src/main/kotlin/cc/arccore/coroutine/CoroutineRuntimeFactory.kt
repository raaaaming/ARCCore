package cc.arccore.coroutine

import cc.arccore.coroutine.dispatcher.ArcDispatchers
import org.bukkit.plugin.Plugin

class CoroutineRuntimeFactory(private val plugin: Plugin) {
    private val dispatchers = ArcDispatchers(plugin)

    fun create(moduleId: String): CoroutineRuntime =
        DefaultCoroutineRuntime(moduleId, dispatchers)
}
