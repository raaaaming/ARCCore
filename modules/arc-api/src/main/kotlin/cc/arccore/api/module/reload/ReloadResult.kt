package cc.arccore.api.module.reload

sealed class ReloadResult {
    abstract val moduleId: String

    data class Success(
        override val moduleId: String,
        val affectedModules: List<String>,
        val elapsedMs: Long
    ) : ReloadResult()

    data class Failure(
        override val moduleId: String,
        val phase: String,
        val error: Throwable,
        val partialResults: List<ModuleReloadOutcome> = emptyList()
    ) : ReloadResult()

    data class PartialSuccess(
        override val moduleId: String,
        val succeededModules: List<String>,
        val failedModules: List<ModuleReloadOutcome>
    ) : ReloadResult()

    data class Rejected(
        override val moduleId: String,
        val reason: String
    ) : ReloadResult()

    data class AlreadyReloading(override val moduleId: String) : ReloadResult()

    val isSuccess: Boolean get() = this is Success
}

data class ModuleReloadOutcome(
    val moduleId: String,
    val success: Boolean,
    val error: Throwable? = null
)
