package cc.arccore.loader.classloader

@FunctionalInterface
fun interface SharedClassFilter {

    fun isShared(className: String): Boolean

    companion object {

        private val DEFAULT_SHARED_PREFIXES = listOf(
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "kotlin.",
            "kotlinx.",
            "org.jetbrains.",
            "org.intellij.",
            "org.bukkit.",
            "io.papermc.",
            "net.kyori.",
            "com.destroystokyo.paper.",
            "org.spigotmc.",
            "cc.arccore.api."
        )

        fun default(): SharedClassFilter = SharedClassFilter { className ->
            DEFAULT_SHARED_PREFIXES.any { prefix ->
                className.startsWith(prefix)
            }
        }

        fun custom(sharedPrefixes: List<String>): SharedClassFilter = SharedClassFilter { className ->
            sharedPrefixes.any { prefix -> className.startsWith(prefix) }
        }

        fun merge(base: SharedClassFilter, additional: SharedClassFilter): SharedClassFilter =
            SharedClassFilter { className ->
                base.isShared(className) || additional.isShared(className)
            }
    }
}
