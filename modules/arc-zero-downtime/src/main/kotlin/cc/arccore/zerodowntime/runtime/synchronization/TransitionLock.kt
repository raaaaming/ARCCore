package cc.arccore.zerodowntime.runtime.synchronization

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

enum class TransitionSignal { ABORT, NONE }

internal class TransitionLock {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    private val signals = ConcurrentHashMap<String, TransitionSignal>()

    fun tryAcquire(moduleId: String, timeoutMs: Long = 0): Boolean {
        val lock = locks.computeIfAbsent(moduleId) { ReentrantLock() }
        return if (timeoutMs <= 0) {
            lock.tryLock()
        } else {
            lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
        }
    }

    fun release(moduleId: String) {
        locks[moduleId]?.let { lock ->
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
        signals.remove(moduleId)
    }

    fun isLocked(moduleId: String): Boolean = locks[moduleId]?.isLocked ?: false

    fun signal(moduleId: String, signal: TransitionSignal) {
        signals[moduleId] = signal
    }

    fun checkSignal(moduleId: String): TransitionSignal = signals[moduleId] ?: TransitionSignal.NONE
}
