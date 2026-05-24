package cc.arccore.bootstrap.runtime.lazy

import kotlin.reflect.KClass

sealed class ServiceWiringResult {

    data class Success(
        val moduleId: String,
        val wiredServices: Map<KClass<*>, Any>,
        val lazyPendingCount: Int,
        val eagerWiredCount: Int
    ) : ServiceWiringResult() {
        val totalWired: Int get() = wiredServices.size
    }

    data class Failure(
        val moduleId: String,
        val cause: Throwable,
        val partiallyWired: Map<KClass<*>, Any> = emptyMap()
    ) : ServiceWiringResult()

    data class Empty(
        val moduleId: String,
        val reason: String = "no services to wire"
    ) : ServiceWiringResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}
