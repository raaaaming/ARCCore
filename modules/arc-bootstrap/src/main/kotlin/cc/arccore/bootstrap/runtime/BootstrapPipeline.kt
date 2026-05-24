package cc.arccore.bootstrap.runtime

import cc.arccore.bootstrap.runtime.state.BootstrapResult

/**
 * Executes the full bootstrap pipeline for a single [BootstrapContext].
 *
 * Each implementation runs all registered phase handlers in the correct order,
 * validates pre/post conditions, profiles timing, and returns a [BootstrapResult].
 */
interface BootstrapPipeline {

    /**
     * Executes all bootstrap phases for [context].
     * Guaranteed to return a non-throwing [BootstrapResult] — all exceptions are caught
     * and wrapped into [BootstrapResult.Failure].
     */
    fun execute(context: BootstrapContext): BootstrapResult
}
