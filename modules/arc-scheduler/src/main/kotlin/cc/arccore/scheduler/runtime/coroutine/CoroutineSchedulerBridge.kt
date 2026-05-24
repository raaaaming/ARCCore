package cc.arccore.scheduler.runtime.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

// CoroutineRuntime (arc-coroutine)과 SchedulerRuntime을 연결하는 브릿지
// arc-coroutine이 없어도 동작 가능하도록 인터페이스 분리

interface CoroutineSchedulerBridge {
    fun launch(block: suspend CoroutineScope.() -> Unit): Job
    fun activeJobCount(): Int
    fun close()
}

// CoroutineRuntime이 있을 때의 구현
class DefaultCoroutineSchedulerBridge(
    private val scope: CoroutineScope,
    private val jobTracker: JobTracker = NoOpJobTracker
) : CoroutineSchedulerBridge {

    override fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return scope.launch {
            jobTracker.increment()
            try { block() } finally { jobTracker.decrement() }
        }
    }

    override fun activeJobCount(): Int = jobTracker.count()

    override fun close() {
        (scope.coroutineContext[Job])?.cancel()
    }
}

interface JobTracker {
    fun count(): Int
    fun increment() {}
    fun decrement() {}
}

private object NoOpJobTracker : JobTracker {
    override fun count() = 0
}

class AtomicJobTracker : JobTracker {
    private val counter = AtomicInteger(0)
    override fun count() = counter.get()
    override fun increment() { counter.incrementAndGet() }
    override fun decrement() { counter.decrementAndGet() }
}
