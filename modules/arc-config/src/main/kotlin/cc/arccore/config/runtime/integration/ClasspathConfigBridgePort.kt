package cc.arccore.config.runtime.integration

interface ClasspathConfigBridgePort {
    fun readClasspathResource(path: String): String?
}
