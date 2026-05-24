package cc.arccore.api.di.exception

open class DIException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class DependencyResolutionException(type: String, reason: String, cause: Throwable? = null)
    : DIException("Cannot resolve '$type': $reason", cause)

class CircularDependencyException(cycle: List<String>)
    : DIException("Circular dependency detected: ${cycle.joinToString(" → ")}")

class MissingDependencyException(type: String, requiredBy: String)
    : DIException("Missing dependency '$type' required by '$requiredBy'")

class InvalidInjectableException(type: String, reason: String)
    : DIException("'$type' is not injectable: $reason")

class ScopeViolationException(type: String, reason: String)
    : DIException("Scope violation for '$type': $reason")

class GeneratedInjectionException(type: String, reason: String, cause: Throwable? = null)
    : DIException("Generated injection failed for '$type': $reason", cause)

class InjectorLoadException(reason: String, cause: Throwable? = null)
    : DIException("Injector load failed: $reason", cause)

class ObjectGraphException(moduleId: String, reason: String, cause: Throwable? = null)
    : DIException("Object graph error in module '$moduleId': $reason", cause)
