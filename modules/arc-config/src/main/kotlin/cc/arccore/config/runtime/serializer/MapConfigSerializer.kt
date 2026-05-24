package cc.arccore.config.runtime.serializer

import cc.arccore.config.runtime.exception.ConfigSerializationException
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Pure reflection-based [ConfigSerializer] with no external dependencies.
 *
 * Serialization: extracts all member properties into a flat Map.
 * Deserialization: uses the primary constructor, matching parameter names to map keys
 * with type coercion. Parameters with defaults are skipped when absent from the data map.
 */
class MapConfigSerializer<T : Any> : ConfigSerializer<T> {

    override fun serialize(value: T): Map<String, Any?> {
        return try {
            value::class.memberProperties.associate { prop ->
                @Suppress("UNCHECKED_CAST")
                prop.name to (prop as kotlin.reflect.KProperty1<Any, *>).get(value)
            }
        } catch (e: Exception) {
            throw ConfigSerializationException("Failed to serialize ${value::class.simpleName}: ${e.message}", e)
        }
    }

    override fun deserialize(data: Map<String, Any?>, clazz: KClass<T>): T {
        val constructor = clazz.primaryConstructor
            ?: throw ConfigSerializationException(
                "Class '${clazz.simpleName}' has no primary constructor — cannot deserialize"
            )

        val args = mutableMapOf<kotlin.reflect.KParameter, Any?>()

        for (param in constructor.parameters) {
            val paramName = param.name
                ?: throw ConfigSerializationException(
                    "Constructor parameter at index ${param.index} in '${clazz.simpleName}' has no name"
                )

            if (data.containsKey(paramName)) {
                val rawValue = data[paramName]
                args[param] = coerce(rawValue, param, clazz.simpleName ?: "Unknown")
            } else if (param.isOptional) {
                // Has a default value — skip to let Kotlin apply it
                continue
            } else if (param.type.isMarkedNullable) {
                args[param] = null
            } else {
                throw ConfigSerializationException(
                    "Required constructor parameter '$paramName' is missing from config data for '${clazz.simpleName}'"
                )
            }
        }

        return try {
            constructor.callBy(args)
        } catch (e: Exception) {
            throw ConfigSerializationException(
                "Failed to instantiate '${clazz.simpleName}': ${e.message}", e
            )
        }
    }

    /**
     * Coerces a raw deserialized value to the target parameter's type.
     *
     * YAML/JSON readers may produce different numeric types (Int vs Long vs Double)
     * depending on the raw text. This method bridges the gap.
     */
    private fun coerce(value: Any?, param: kotlin.reflect.KParameter, className: String): Any? {
        if (value == null) return null
        val classifier = param.type.classifier as? KClass<*> ?: return value

        return try {
            when (classifier) {
                String::class -> value.toString()
                Int::class -> when (value) {
                    is Int -> value
                    is Long -> value.toInt()
                    is Double -> value.toInt()
                    is Float -> value.toInt()
                    is Number -> value.toInt()
                    is String -> value.toInt()
                    else -> value
                }
                Long::class -> when (value) {
                    is Long -> value
                    is Int -> value.toLong()
                    is Double -> value.toLong()
                    is Float -> value.toLong()
                    is Number -> value.toLong()
                    is String -> value.toLong()
                    else -> value
                }
                Double::class -> when (value) {
                    is Double -> value
                    is Float -> value.toDouble()
                    is Int -> value.toDouble()
                    is Long -> value.toDouble()
                    is Number -> value.toDouble()
                    is String -> value.toDouble()
                    else -> value
                }
                Float::class -> when (value) {
                    is Float -> value
                    is Double -> value.toFloat()
                    is Int -> value.toFloat()
                    is Long -> value.toFloat()
                    is Number -> value.toFloat()
                    is String -> value.toFloat()
                    else -> value
                }
                Boolean::class -> when (value) {
                    is Boolean -> value
                    is String -> value.lowercase() == "true"
                    else -> value
                }
                Short::class -> when (value) {
                    is Short -> value
                    is Number -> value.toShort()
                    is String -> value.toShort()
                    else -> value
                }
                Byte::class -> when (value) {
                    is Byte -> value
                    is Number -> value.toByte()
                    is String -> value.toByte()
                    else -> value
                }
                else -> value
            }
        } catch (e: NumberFormatException) {
            throw ConfigSerializationException(
                "Failed to coerce value '$value' to ${classifier.simpleName} for parameter '${param.name}' in '$className': ${e.message}",
                e
            )
        }
    }
}
