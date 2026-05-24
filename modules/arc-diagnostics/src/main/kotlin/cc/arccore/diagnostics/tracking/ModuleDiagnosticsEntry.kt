package cc.arccore.diagnostics.tracking

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ModuleDiagnosticsEntry(val moduleId: String) {
    private val _loadedAt = AtomicLong(-1L)
    private val _enabledAt = AtomicLong(-1L)
    private val _reloadGeneration = AtomicInteger(0)
    private val _lastReloadAt = AtomicLong(-1L)

    val loadedAt: Long? get() = _loadedAt.get().takeIf { it >= 0L }
    val enabledAt: Long? get() = _enabledAt.get().takeIf { it >= 0L }
    val reloadGeneration: Int get() = _reloadGeneration.get()
    val lastReloadAt: Long? get() = _lastReloadAt.get().takeIf { it >= 0L }

    fun onLoaded() {
        val now = System.currentTimeMillis()
        if (_loadedAt.get() >= 0L) {
            // 재로드 시
            _reloadGeneration.incrementAndGet()
            _lastReloadAt.set(now)
            _enabledAt.set(-1L)
        }
        _loadedAt.set(now)
    }

    fun onEnabled() {
        _enabledAt.set(System.currentTimeMillis())
    }

    fun onUnloaded() {
        // 항목은 유지하되 활성 상태 플래그만 초기화 (히스토리 보존)
        _enabledAt.set(-1L)
    }

    fun onFailed() {
        _enabledAt.set(-1L)
    }
}
