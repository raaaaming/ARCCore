package cc.arccore.config.runtime.serializer

import kotlin.reflect.KClass

interface ConfigSerializer<T : Any> {
    fun serialize(value: T): Map<String, Any?>
    fun deserialize(data: Map<String, Any?>, clazz: KClass<T>): T
}
