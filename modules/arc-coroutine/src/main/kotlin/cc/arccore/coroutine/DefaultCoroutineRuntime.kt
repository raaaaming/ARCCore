package cc.arccore.coroutine

import cc.arccore.coroutine.dispatcher.ArcDispatchers
import cc.arccore.coroutine.dispatcher.ModuleDispatcher
import cc.arccore.coroutine.supervision.ModuleExceptionHandler
import cc.arccore.coroutine.tracking.CoroutineJobTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultCoroutineRuntime(
    override val moduleId: String,
    private val dispatchers: ArcDispatchers
) : CoroutineRuntime {

    private val exceptionHandler = ModuleExceptionHandler.create(moduleId)
    private val tracker = CoroutineJobTracker()

    override val moduleScope: ModuleCoroutineScope = DefaultModuleCoroutineScope(
        baseContext = Dispatchers.Default,
        exceptionHandler = exceptionHandler
    )

    override fun launch(
        dispatcher: ModuleDispatcher,
        block: suspend CoroutineScope.() -> Unit
    ): Job = moduleScope.scope.launch(dispatchers.forDispatcher(dispatcher)) {
        tracker.increment()
        try {
            block()
        } finally {
            tracker.decrement()
        }
    }

    override fun <T> async(
        dispatcher: ModuleDispatcher,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> = moduleScope.scope.async(dispatchers.forDispatcher(dispatcher)) {
        tracker.increment()
        try {
            block()
        } finally {
            tracker.decrement()
        }
    }

    override suspend fun <T> onSync(block: suspend CoroutineScope.() -> T): T =
        withContext(dispatchers.sync) { block() }

    override fun activeJobCount(): Int = tracker.count()

    override fun activeTaskCount(): Int = activeJobCount()

    override fun close() {
        moduleScope.cancel("Module '$moduleId' unloaded — coroutine scope cancelled")
    }
}
