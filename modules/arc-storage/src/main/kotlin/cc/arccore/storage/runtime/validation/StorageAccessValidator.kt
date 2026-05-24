package cc.arccore.storage.runtime.validation

import cc.arccore.storage.runtime.exception.InvalidStorageAccessException

/**
 * Utility object for validating storage access parameters before handle creation.
 */
object StorageAccessValidator {

    private val UNSAFE_PATH_PATTERNS = listOf(
        "..",
        "//",
        "\\"
    )

    /**
     * Validates that [path] is a safe relative path for file storage.
     *
     * Rejects paths containing path-traversal sequences (`..`), absolute path
     * indicators (`//`, `\`), or blank values.
     *
     * @throws InvalidStorageAccessException if [path] is unsafe.
     */
    fun validatePath(path: String) {
        if (path.isBlank()) {
            throw InvalidStorageAccessException(
                "Storage path must not be blank."
            )
        }
        UNSAFE_PATH_PATTERNS.forEach { pattern ->
            if (path.contains(pattern)) {
                throw InvalidStorageAccessException(
                    "Storage path '$path' contains unsafe sequence '$pattern'."
                )
            }
        }
    }

    /**
     * Validates that [key] is a non-blank config key.
     *
     * @throws InvalidStorageAccessException if [key] is blank.
     */
    fun validateConfigKey(key: String) {
        if (key.isBlank()) {
            throw InvalidStorageAccessException(
                "Config key must not be blank."
            )
        }
    }

    /**
     * Returns a [StorageValidationResult] for [path] without throwing.
     */
    fun checkPath(path: String): StorageValidationResult =
        runCatching { validatePath(path) }
            .fold(
                onSuccess = { StorageValidationResult.Valid },
                onFailure = { StorageValidationResult.Invalid(it.message ?: "Invalid path") }
            )
}
