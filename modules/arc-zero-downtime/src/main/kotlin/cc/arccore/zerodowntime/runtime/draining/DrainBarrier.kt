package cc.arccore.zerodowntime.runtime.draining

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DrainBarrier(val moduleId: String) : InflightCounter {
    private val counter = AtomicInteger(0)
    private val semaphore = Semaphore(0)

    fun acquire(): DrainToken {
        counter.incrementAndGet()
        return DrainToken(this)
    }

    internal fun release() {
        if (counter.decrementAndGet() == 0) {
            semaphore.release()
        }
    }

    override fun currentCount(): Int = counter.get()

    override fun waitForZero(timeoutMs: Long): Boolean {
        if (counter.get() == 0) return true
        return semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
    }

    fun forceRelease() {
        counter.set(0)
        semaphore.release()
    }
}

class DrainToken(private val barrier: DrainBarrier) : AutoCloseable {
    override fun close() { barrier.release() }
}
