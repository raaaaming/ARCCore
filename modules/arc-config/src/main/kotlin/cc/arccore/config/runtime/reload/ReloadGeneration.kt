package cc.arccore.config.runtime.reload

import java.util.concurrent.atomic.AtomicLong

class ReloadGeneration {
    private val counter = AtomicLong(0L)

    fun current(): Long = counter.get()

    fun increment(): Long = counter.incrementAndGet()

    fun isStale(handleGeneration: Long): Boolean = handleGeneration != counter.get()
}
