package cc.arccore.zerodowntime.runtime.synchronization

import java.util.concurrent.locks.StampedLock

internal class ReadWriteGate(val moduleId: String) {
    private val lock = StampedLock()

    @Volatile private var closed = false

    fun acquireRead(): ReadGateToken {
        val stamp = lock.readLock()
        return ReadGateToken(lock, stamp)
    }

    fun acquireWrite(): WriteGateToken {
        val stamp = lock.writeLock()
        closed = true
        return WriteGateToken(lock, stamp, this)
    }

    internal fun releaseWrite(stamp: Long) {
        lock.unlockWrite(stamp)
        closed = false
    }

    fun isGateClosed(): Boolean = closed

    class ReadGateToken(private val lock: StampedLock, private val stamp: Long) : AutoCloseable {
        override fun close() { lock.unlockRead(stamp) }
    }

    class WriteGateToken(
        private val lock: StampedLock,
        private val stamp: Long,
        private val gate: ReadWriteGate
    ) : AutoCloseable {
        override fun close() { gate.releaseWrite(stamp) }
    }
}
