package cc.arccore.api.module

import cc.arccore.api.module.description.version.ModuleVersion
import java.util.concurrent.atomic.AtomicBoolean

data class ModuleDescription(
    val id: String,
    val name: String,
    val version: ModuleVersion,
    val description: String,
    val authors: List<String>,
    val dependencies: List<ModuleDependency>,
    val softDependencies: List<ModuleDependency>,
    val loadBefore: List<String>,
    val dependPlugins: List<String>,
    val libraries: List<String>,
    val mainClass: String,
    val apiVersion: String,
    val website: String
) {
    companion object {
        val EMPTY = ModuleDescription(
            id = "",
            name = "",
            version = ModuleVersion(0, 0, 0),
            description = "",
            authors = emptyList(),
            dependencies = emptyList(),
            softDependencies = emptyList(),
            loadBefore = emptyList(),
            dependPlugins = emptyList(),
            libraries = emptyList(),
            mainClass = "",
            apiVersion = "1.0",
            website = ""
        )

        @JvmStatic
        fun fromJson(json: String): ModuleDescription {
            return ParserInvoker.parse(json)
        }

        @JvmStatic
        fun toJson(description: ModuleDescription): String {
            return ParserInvoker.serialize(description)
        }
    }
}

object ParserInvoker {
    @Volatile
    private var parser: ((String) -> ModuleDescription)? = null

    @Volatile
    private var serializer: ((ModuleDescription) -> String)? = null

    private val registered = AtomicBoolean(false)

    fun register(
        parseFn: (String) -> ModuleDescription,
        serializeFn: (ModuleDescription) -> String
    ) {
        if (!registered.compareAndSet(false, true)) return
        parser = parseFn
        serializer = serializeFn
    }

    internal fun parse(json: String): ModuleDescription {
        return parser?.invoke(json)
            ?: throw UnsupportedOperationException(
                "ModuleDescription JSON parser not registered. " +
                    "Ensure arc-loader is on the classpath and initialized."
            )
    }

    internal fun serialize(description: ModuleDescription): String {
        return serializer?.invoke(description)
            ?: throw UnsupportedOperationException(
                "ModuleDescription JSON serializer not registered. " +
                    "Ensure arc-loader is on the classpath and initialized."
            )
    }
}
