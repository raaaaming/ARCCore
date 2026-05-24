package cc.arccore.config.runtime.exception

class ConfigReloadException(val path: String, message: String, cause: Throwable? = null) : ConfigRuntimeException(message, cause)
