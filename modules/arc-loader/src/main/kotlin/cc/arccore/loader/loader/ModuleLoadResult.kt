package cc.arccore.loader.loader

import cc.arccore.api.module.ModuleContainer
import java.nio.file.Path

sealed class ModuleLoadResult {

    abstract val jarPath: Path

    data class Success(
        override val jarPath: Path,
        val container: ModuleContainer
    ) : ModuleLoadResult()

    data class Failure(
        override val jarPath: Path,
        val error: Throwable,
        val moduleId: String? = null
    ) : ModuleLoadResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): ModuleContainer? = (this as? Success)?.container

    fun exceptionOrNull(): Throwable? = (this as? Failure)?.error
}
