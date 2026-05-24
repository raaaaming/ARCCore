package cc.arccore.di.container

import cc.arccore.api.di.Inject
import cc.arccore.api.di.ResolutionContext
import cc.arccore.api.di.exception.DIException
import cc.arccore.api.di.exception.DependencyResolutionException
import cc.arccore.api.di.exception.InvalidInjectableException
import cc.arccore.api.di.exception.MissingDependencyException
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass

class ConstructorInjector {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> instantiate(type: KClass<T>, context: ResolutionContext): T {
        val ctor = findInjectableConstructor(type)
        val typeName = type.qualifiedName ?: type.simpleName ?: "Unknown"
        val args = ctor.parameterTypes.map { paramType ->
            if (paramType.isPrimitive || paramType.isArray) {
                throw InvalidInjectableException(
                    typeName,
                    "Constructor parameter '${paramType.name}' is a primitive or array type and cannot be injected"
                )
            }
            try {
                @Suppress("UNCHECKED_CAST")
                context.resolve(paramType.kotlin as KClass<Any>)
            } catch (e: DIException) {
                throw e
            } catch (e: Exception) {
                throw MissingDependencyException(paramType.name, typeName)
            }
        }.toTypedArray()
        return try {
            ctor.newInstance(*args) as T
        } catch (e: IllegalAccessException) {
            throw InvalidInjectableException(
                type.qualifiedName ?: type.simpleName ?: "Unknown",
                "Constructor is not accessible"
            )
        } catch (e: InvocationTargetException) {
            val cause = e.cause ?: e
            if (cause is DIException) throw cause
            throw DependencyResolutionException(
                type.qualifiedName ?: type.simpleName ?: "Unknown",
                "Constructor threw an exception: ${cause.message}",
                cause
            )
        }
    }

    fun canInstantiate(type: KClass<*>): Boolean = try {
        findInjectableConstructor(type)
        true
    } catch (_: InvalidInjectableException) {
        false
    }

    private fun findInjectableConstructor(type: KClass<*>): java.lang.reflect.Constructor<*> {
        val javaClass = type.java
        val typeName = type.qualifiedName ?: type.simpleName ?: "Unknown"
        val injectAnnotated = javaClass.constructors.filter {
            it.isAnnotationPresent(Inject::class.java)
        }
        return when {
            injectAnnotated.size == 1 -> injectAnnotated.first()
            injectAnnotated.size > 1 -> throw InvalidInjectableException(
                typeName,
                "Multiple constructors annotated with @Inject"
            )
            javaClass.constructors.size == 1 -> javaClass.constructors.first()
            else -> throw InvalidInjectableException(
                typeName,
                "No @Inject constructor found and multiple constructors exist"
            )
        }
    }
}
