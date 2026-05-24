package cc.arccore.config.runtime.integration

interface StorageConfigBridgePort {
    fun readFile(moduleId: String, relativePath: String): String?
    fun writeFile(moduleId: String, relativePath: String, content: String)
    fun fileExists(moduleId: String, relativePath: String): Boolean
}
