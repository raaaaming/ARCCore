package cc.arccore.bootstrap.runtime

enum class BootstrapPhase(val isPre: Boolean, val isRuntime: Boolean) {
    DISCOVERY(isPre = true, isRuntime = false),
    METADATA_PRELOAD(isPre = true, isRuntime = false),
    DEPENDENCY_GRAPH_BUILD(isPre = true, isRuntime = false),
    CLASSLOADER_PREPARE(isPre = true, isRuntime = false),
    GENERATED_BOOTSTRAP(isPre = false, isRuntime = true),
    SERVICE_WIRING(isPre = false, isRuntime = true),
    ENABLE_PHASE(isPre = false, isRuntime = true),
    POST_ENABLE(isPre = false, isRuntime = true),
    FAILED(isPre = false, isRuntime = false),
    SKIPPED(isPre = false, isRuntime = false);

    val isTerminal: Boolean get() = this == FAILED || this == SKIPPED
    val isActive: Boolean get() = !isTerminal

    /**
     * Returns the next active phase in ordinal order, skipping terminal phases.
     * Returns null when there is no next phase (end of active phases).
     */
    fun next(): BootstrapPhase? {
        val values = entries
        val nextOrdinal = ordinal + 1
        // FAILED and SKIPPED are terminal — exclude from next()
        if (nextOrdinal >= values.size - 2) return null
        return values[nextOrdinal]
    }

    /**
     * Whether this phase can be safely skipped during hot-reload to reduce overhead.
     */
    fun canReloadSkip(): Boolean = this == DISCOVERY || this == CLASSLOADER_PREPARE
}
