package cc.arccore.api.module

/**
 * Simple logging interface provided to modules via [ModuleContext].
 *
 * Implementations are backed by the platform's logging facility
 * (e.g. Java Util Logging, SLF4J, or a custom bridge).
 */
interface ModuleLogger {

    fun info(message: String)

    fun warn(message: String)

    fun error(message: String, throwable: Throwable? = null)

    fun debug(message: String)

    fun trace(message: String)

    /**
     * Creates a derived logger that prefixes all messages with [prefix].
     * Useful for sub-components within a module.
     */
    fun withPrefix(prefix: String): ModuleLogger
}
