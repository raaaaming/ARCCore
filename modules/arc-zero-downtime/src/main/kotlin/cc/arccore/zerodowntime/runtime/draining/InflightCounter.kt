package cc.arccore.zerodowntime.runtime.draining

interface InflightCounter {
    fun currentCount(): Int
    fun waitForZero(timeoutMs: Long): Boolean
}

class SimpleInflightCounter : InflightCounter {
    override fun currentCount() = 0
    override fun waitForZero(timeoutMs: Long) = true
}

class CompositeInflightCounter(
    private val counters: List<InflightCounter>
) : InflightCounter {
    override fun currentCount() = counters.sumOf { it.currentCount() }
    override fun waitForZero(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (currentCount() == 0) return true
            Thread.sleep(50)
        }
        return currentCount() == 0
    }
}
