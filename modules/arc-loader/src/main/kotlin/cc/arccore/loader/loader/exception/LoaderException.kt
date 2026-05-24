package cc.arccore.loader.loader.exception

open class LoaderException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class ModuleDiscoveryException(
    message: String,
    cause: Throwable? = null
) : LoaderException(message, cause)

class ModuleInstantiationException(
    message: String,
    cause: Throwable? = null
) : LoaderException(message, cause)

class DuplicateModuleException(
    val moduleId: String,
    val existingJar: String,
    val duplicateJar: String
) : LoaderException(
    "Duplicate module id '$moduleId' in jars: $existingJar, $duplicateJar"
)
