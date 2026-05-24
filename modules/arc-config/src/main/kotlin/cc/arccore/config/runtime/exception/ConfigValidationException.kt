package cc.arccore.config.runtime.exception

class ConfigValidationException(
    val path: String,
    val errors: List<String>,
    cause: Throwable? = null
) : ConfigRuntimeException("Config validation failed for '$path': ${errors.joinToString("; ")}", cause)
