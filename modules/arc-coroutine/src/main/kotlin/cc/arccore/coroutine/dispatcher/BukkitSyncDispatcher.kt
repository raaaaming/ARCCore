package cc.arccore.coroutine.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext

// Bukkit 메인 스레드로 코루틴을 bridge하는 디스패처.
// 이미 메인 스레드이면 즉시 실행, 아니면 BukkitScheduler.runTask로 스케줄.
class BukkitSyncDispatcher(private val plugin: Plugin) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            plugin.server.scheduler.runTask(plugin, block)
        }
    }
}
