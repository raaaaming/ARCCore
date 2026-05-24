package cc.arccore.config.runtime.reload

sealed class ReloadResult {
    data class Success(
        val path: String,
        val newGeneration: Long,
        val durationNanos: Long = 0L
    ) : ReloadResult()

    data class Failure(
        val path: String,
        val cause: Throwable,
        val validationErrors: List<String> = emptyList()
    ) : ReloadResult()

    data class NoChange(val reason: String) : ReloadResult()

    val isSuccess: Boolean get() = this is Success
}
