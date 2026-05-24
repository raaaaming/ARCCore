package cc.arccore.coroutine.tracking

import java.util.concurrent.atomic.AtomicInteger

class CoroutineJobTracker {
    private val _count = AtomicInteger(0)

    fun increment() = _count.incrementAndGet()
    fun decrement() = _count.decrementAndGet()
    fun count(): Int = _count.get()
}
