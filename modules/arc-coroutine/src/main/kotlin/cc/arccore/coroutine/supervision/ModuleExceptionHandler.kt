package cc.arccore.coroutine.supervision

import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.logging.Logger

object ModuleExceptionHandler {
    fun create(moduleId: String): CoroutineExceptionHandler {
        val log = Logger.getLogger("ARCCore.Coroutine.$moduleId")
        return CoroutineExceptionHandler { _, throwable ->
            log.severe("Unhandled exception in coroutine of module '$moduleId': ${throwable.message}")
            log.severe(throwable.stackTraceToString())
        }
    }
}
