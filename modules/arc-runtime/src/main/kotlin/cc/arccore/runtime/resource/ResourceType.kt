package cc.arccore.runtime.resource

/**
 * Classifies the kind of runtime resource being tracked.
 *
 * Why classification matters:
 *   Different resource types have different risk profiles. An EXECUTOR holds OS threads that
 *   capture their creation-time ClassLoader context. A COROUTINE_SCOPE holds a reference graph
 *   to every lambda that was launched inside it. A LISTENER is registered in Bukkit's global
 *   HandlerList and will outlive the module unless explicitly unregistered.
 *   Knowing the type lets the tracker prioritize cleanup order and generate targeted leak warnings.
 */
enum class ResourceType {
    LISTENER,
    COMMAND,
    SERVICE,
    COROUTINE_SCOPE,
    COROUTINE_JOB,
    SCHEDULER_TASK,
    EXECUTOR,
    CACHE,
    FILE,
    DATABASE,
    NETWORK,
    CUSTOM;

    val isCritical: Boolean
        get() = this in setOf(EXECUTOR, COROUTINE_SCOPE, LISTENER, DATABASE)
}
