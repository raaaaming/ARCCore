package cc.arccore.coroutine.dispatcher

// 디스패처 선택을 위한 sealed class
sealed class ModuleDispatcher {
    // CPU-bound / 기본 비동기 (Dispatchers.Default)
    object Async : ModuleDispatcher()
    // 블로킹 I/O (Dispatchers.IO)
    object IO : ModuleDispatcher()
    // Bukkit 메인 스레드 (BukkitSyncDispatcher)
    object Sync : ModuleDispatcher()
    // 미래 확장: per-tick, virtual thread, distributed 등
    object Tick : ModuleDispatcher()
}
