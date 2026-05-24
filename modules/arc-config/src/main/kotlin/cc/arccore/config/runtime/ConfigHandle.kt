package cc.arccore.config.runtime

interface ConfigHandle<T : Any> : AutoCloseable {
    val moduleId: String
    val configPath: String
    val generation: Long
    val isOpen: Boolean
    fun get(): T
    fun isStale(): Boolean
}
