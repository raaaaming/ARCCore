package cc.arccore.runtime.annotation.validation

import cc.arccore.runtime.annotation.exception.InvalidAnnotatedClassException
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class AnnotatedClassValidator {

    fun validate(
        clazz: Class<*>,
        annotation: KClass<out Annotation>,
        expectedSupertype: KClass<*>? = null
    ) {
        val annotationName = annotation.simpleName ?: annotation.qualifiedName

        if (clazz.isInterface) {
            throw InvalidAnnotatedClassException(
                "Class '${clazz.name}' annotated with @$annotationName must not be an interface."
            )
        }

        if (Modifier.isAbstract(clazz.modifiers)) {
            throw InvalidAnnotatedClassException(
                "Class '${clazz.name}' annotated with @$annotationName must not be abstract."
            )
        }

        if (expectedSupertype != null && !expectedSupertype.java.isAssignableFrom(clazz)) {
            throw InvalidAnnotatedClassException(
                "Class '${clazz.name}' annotated with @$annotationName must implement or extend '${expectedSupertype.qualifiedName}'."
            )
        }

        try {
            clazz.getConstructor()
        } catch (_: NoSuchMethodException) {
            throw InvalidAnnotatedClassException(
                "Class '${clazz.name}' annotated with @$annotationName must have a public no-arg constructor."
            )
        }
    }
}
