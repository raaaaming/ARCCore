package cc.arccore.loader.metadata.exception

import cc.arccore.loader.metadata.validation.MetadataValidationError

open class MetadataException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class MetadataReadException(
    message: String,
    cause: Throwable? = null
) : MetadataException(message, cause)

class MetadataValidationException(
    message: String,
    val errors: List<MetadataValidationError>
) : MetadataException(message)
