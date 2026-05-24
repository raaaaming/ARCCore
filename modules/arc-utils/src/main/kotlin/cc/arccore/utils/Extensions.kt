package cc.arccore.utils

import java.nio.file.Path

fun Path.resolveOrNull(vararg parts: String): Path {
    return parts.fold(this) { acc, part -> acc.resolve(part) }
}

inline fun <reified T : Any> T?.requireNotNull(lazyMessage: () -> Any): T {
    return this ?: throw IllegalStateException(lazyMessage().toString())
}
