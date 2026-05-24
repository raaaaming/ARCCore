package cc.arccore.runtime.unload.leak

import cc.arccore.loader.ModuleClassLoader
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * 모듈 ClassLoader의 GC 수집 여부를 PhantomReference로 추적한다.
 *
 * 언로드 후 [leakCheckDelaySeconds] 경과 시점에 GC 수집 여부를 확인하며,
 * 미수집 시 경고 로그와 함께 누수 힌트를 제공한다.
 *
 * [gcHintEnabled] = true 이면 체크 직전 System.gc()를 호출한다.
 * 프로덕션에서는 false 권장.
 *
 * 이 클래스는 ClassLoader에 대한 강한 참조를 절대 보유하지 않는다.
 */
class ModuleClassLoaderTracker(
    val leakCheckDelaySeconds: Long = 30L,
    val gcHintEnabled: Boolean = false
) : AutoCloseable {

    private val log = Logger.getLogger(ModuleClassLoaderTracker::class.java.name)
    private val refQueue = ReferenceQueue<ModuleClassLoader>()
    private val tracked = ConcurrentHashMap<String, TrackedEntry>()
    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "arc-classloader-leak-checker").apply { isDaemon = true }
        }

    fun track(moduleId: String, classLoader: ModuleClassLoader) {
        val entry = TrackedEntry(
            moduleId = moduleId,
            ref = PhantomReference(classLoader, refQueue)
        )
        tracked[moduleId] = entry
        scheduler.schedule({ checkForLeak(moduleId) }, leakCheckDelaySeconds, TimeUnit.SECONDS)
    }

    private fun checkForLeak(moduleId: String) {
        if (gcHintEnabled) {
            @Suppress("ExplicitGarbageCollectionCall")
            System.gc()
        }
        drainQueue()

        if (tracked[moduleId] == null) return

        val threadHints = findThreadsHoldingTccl(moduleId)
        log.warning(buildLeakHint(moduleId, threadHints))
    }

    private fun drainQueue() {
        var ref = refQueue.poll()
        while (ref != null) {
            tracked.entries.removeIf { it.value.ref === ref }
            ref = refQueue.poll()
        }
    }

    private fun findThreadsHoldingTccl(moduleId: String): List<String> {
        val result = mutableListOf<String>()
        val rootGroup = getRootThreadGroup()
        val threads = arrayOfNulls<Thread>(rootGroup.activeCount() * 2 + 16)
        val count = rootGroup.enumerate(threads, true)
        for (i in 0 until count) {
            val t = threads[i] ?: continue
            val tccl = t.contextClassLoader
            if (tccl is ModuleClassLoader && tccl.moduleId == moduleId) {
                result.add("Thread[${t.name}] holds TCCL → ModuleClassLoader[$moduleId]")
            }
        }
        return result
    }

    private fun getRootThreadGroup(): ThreadGroup {
        var tg = Thread.currentThread().threadGroup
        while (tg.parent != null) tg = tg.parent!!
        return tg
    }

    private fun buildLeakHint(moduleId: String, threadHints: List<String>): String =
        buildString {
            appendLine("[ModuleClassLoaderTracker] LEAK DETECTED: ModuleClassLoader[$moduleId]")
            appendLine("  ClassLoader was not GC-collected ${leakCheckDelaySeconds}s after unload.")
            if (threadHints.isNotEmpty()) {
                appendLine("  Known strong reference holders (TCCL):")
                threadHints.forEach { appendLine("    - $it") }
            } else {
                appendLine("  No Thread TCCL references found.")
                appendLine("  Check: static fields, Bukkit event listeners not unregistered,")
                appendLine("         Paper scheduler tasks not cancelled, ServiceLoader cache,")
                appendLine("         or objects captured in closures/lambdas.")
            }
        }.trimEnd()

    override fun close() {
        scheduler.shutdownNow()
        tracked.clear()
    }

    private data class TrackedEntry(
        val moduleId: String,
        val ref: PhantomReference<ModuleClassLoader>
    )
}
