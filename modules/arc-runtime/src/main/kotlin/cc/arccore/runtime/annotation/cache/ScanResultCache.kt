package cc.arccore.runtime.annotation.cache

import cc.arccore.runtime.annotation.scanner.ScanResult
import java.util.concurrent.ConcurrentHashMap

class ScanResultCache {

    private val cache = ConcurrentHashMap<String, List<ScanResult>>()

    fun get(moduleId: String): List<ScanResult>? = cache[moduleId]

    fun put(moduleId: String, results: List<ScanResult>) {
        cache[moduleId] = results
    }

    fun invalidate(moduleId: String) {
        cache.remove(moduleId)
    }

    fun clear() {
        cache.clear()
    }

    fun contains(moduleId: String): Boolean = cache.containsKey(moduleId)
}
