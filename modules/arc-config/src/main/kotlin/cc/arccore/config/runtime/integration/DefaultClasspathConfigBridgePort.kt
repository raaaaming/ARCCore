package cc.arccore.config.runtime.integration

/**
 * Reads resources from the context class loader's classpath.
 *
 * Useful for bundling default config files inside a module jar, which are
 * copied to disk when not yet present.
 */
class DefaultClasspathConfigBridgePort : ClasspathConfigBridgePort {

    override fun readClasspathResource(path: String): String? {
        val cl = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
        return cl.getResourceAsStream(path)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }
    }
}
