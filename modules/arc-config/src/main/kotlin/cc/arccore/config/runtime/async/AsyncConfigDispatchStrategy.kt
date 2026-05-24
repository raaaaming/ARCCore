package cc.arccore.config.runtime.async

fun interface AsyncConfigDispatchStrategy {
    fun dispatch(block: suspend () -> Unit)
}
