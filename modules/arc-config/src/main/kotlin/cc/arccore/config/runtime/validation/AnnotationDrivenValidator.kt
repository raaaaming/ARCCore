package cc.arccore.config.runtime.validation

import cc.arccore.config.runtime.validation.annotations.ARCConfigNotNull
import cc.arccore.config.runtime.validation.annotations.ARCConfigPattern
import cc.arccore.config.runtime.validation.annotations.ARCConfigRange
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Validates a config data class instance using reflection-based annotation inspection.
 *
 * Supported constraints:
 * - [ARCConfigRange]: Asserts that numeric property values are within [min, max].
 * - [ARCConfigNotNull]: Asserts that the property value is not null.
 * - [ARCConfigPattern]: Asserts that a String property matches the given regex.
 */
class AnnotationDrivenValidator<T : Any>(
    private val clazz: KClass<T>
) : ConfigValidator<T> {

    override fun validate(value: T): ValidationResult {
        val errors = mutableListOf<String>()

        for (prop in clazz.memberProperties) {
            val propValue = try {
                prop.get(value)
            } catch (e: Exception) {
                errors.add("Failed to read property '${prop.name}': ${e.message}")
                continue
            }

            // @ARCConfigNotNull
            prop.annotations
                .filterIsInstance<ARCConfigNotNull>()
                .forEach { _ ->
                    if (propValue == null) {
                        errors.add("Property '${prop.name}' must not be null (@ARCConfigNotNull)")
                    }
                }

            // @ARCConfigRange
            prop.annotations
                .filterIsInstance<ARCConfigRange>()
                .forEach { annotation ->
                    if (propValue != null) {
                        val numericValue: Double? = when (propValue) {
                            is Int -> propValue.toDouble()
                            is Long -> propValue.toDouble()
                            is Float -> propValue.toDouble()
                            is Double -> propValue
                            is Short -> propValue.toDouble()
                            is Byte -> propValue.toDouble()
                            else -> null
                        }
                        if (numericValue != null) {
                            if (numericValue < annotation.min) {
                                errors.add(
                                    "Property '${prop.name}' value $numericValue is below minimum ${annotation.min} (@ARCConfigRange)"
                                )
                            }
                            if (numericValue > annotation.max) {
                                errors.add(
                                    "Property '${prop.name}' value $numericValue exceeds maximum ${annotation.max} (@ARCConfigRange)"
                                )
                            }
                        }
                    }
                }

            // @ARCConfigPattern
            prop.annotations
                .filterIsInstance<ARCConfigPattern>()
                .forEach { annotation ->
                    if (propValue != null) {
                        val strValue = propValue as? String
                        if (strValue != null) {
                            val regex = try {
                                Regex(annotation.regex)
                            } catch (e: Exception) {
                                errors.add(
                                    "Property '${prop.name}': invalid regex pattern '${annotation.regex}': ${e.message}"
                                )
                                null
                            }
                            if (regex != null && !regex.containsMatchIn(strValue)) {
                                errors.add(
                                    "Property '${prop.name}' value '$strValue' does not match pattern '${annotation.regex}' (@ARCConfigPattern)"
                                )
                            }
                        }
                    }
                }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
