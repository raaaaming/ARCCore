package cc.arccore.loader.metadata.validation

import cc.arccore.api.module.ModuleDependency
import cc.arccore.api.module.ModuleDescription
import cc.arccore.api.module.description.version.ModuleVersion
import cc.arccore.loader.metadata.ArcModuleManifest

object MetadataValidator {

    private val SUPPORTED_API_VERSIONS = setOf("1.0")

    private val MODULE_ID_REGEX = Regex("^[a-z][a-z0-9-]*$")

    fun validate(manifest: ArcModuleManifest): MetadataValidationResult {
        val errors = mutableListOf<MetadataValidationError>()

        if (manifest.id.isBlank()) {
            errors.add(
                MetadataValidationError("id", "Module id must not be blank", ValidationSeverity.ERROR)
            )
        } else if (!MODULE_ID_REGEX.matches(manifest.id)) {
            errors.add(
                MetadataValidationError(
                    "id",
                    "Module id '${manifest.id}' must be lowercase kebab-case (e.g. 'my-module')",
                    ValidationSeverity.ERROR
                )
            )
        }

        if (manifest.main.isBlank()) {
            errors.add(
                MetadataValidationError("main", "Module main class must not be blank", ValidationSeverity.ERROR)
            )
        }

        val version: ModuleVersion? = try {
            ModuleVersion.parse(manifest.version)
        } catch (e: IllegalArgumentException) {
            errors.add(
                MetadataValidationError(
                    "version",
                    "Invalid semantic version: '${manifest.version}'. ${e.message}",
                    ValidationSeverity.ERROR
                )
            )
            null
        }

        if (manifest.apiVersion.isBlank()) {
            errors.add(
                MetadataValidationError("apiVersion", "apiVersion must not be blank", ValidationSeverity.ERROR)
            )
        } else if (manifest.apiVersion !in SUPPORTED_API_VERSIONS) {
            errors.add(
                MetadataValidationError(
                    "apiVersion",
                    "Unsupported API version '${manifest.apiVersion}'. " +
                        "Supported versions: ${SUPPORTED_API_VERSIONS.joinToString(", ")}",
                    ValidationSeverity.WARNING
                )
            )
        }

        val depIds = manifest.depends.map { it.split(":", limit = 2).first().trim() }

        for (dep in manifest.depends) {
            val depId = dep.split(":", limit = 2).first().trim()
            if (depId == manifest.id) {
                errors.add(
                    MetadataValidationError(
                        "depends",
                        "Module '$depId' cannot depend on itself",
                        ValidationSeverity.ERROR
                    )
                )
            }
        }

        val duplicateDepIds = depIds.groupBy { it }.filter { it.value.size > 1 }.keys
        for (dupId in duplicateDepIds) {
            errors.add(
                MetadataValidationError(
                    "depends",
                    "Duplicate dependency '$dupId' in depends list",
                    ValidationSeverity.ERROR
                )
            )
        }

        val softDepIds = manifest.softDepends.map { it.split(":", limit = 2).first().trim() }
        val hardDepIds = depIds.toSet()
        val overlap = hardDepIds.intersect(softDepIds.toSet())
        for (overlapId in overlap) {
            errors.add(
                MetadataValidationError(
                    "softDepends",
                    "Module '$overlapId' appears in both depends and softDepends",
                    ValidationSeverity.ERROR
                )
            )
        }

        val duplicateSoftIds = softDepIds.groupBy { it }.filter { it.value.size > 1 }.keys
        for (dupId in duplicateSoftIds) {
            errors.add(
                MetadataValidationError(
                    "softDepends",
                    "Duplicate dependency '$dupId' in softDepends list",
                    ValidationSeverity.ERROR
                )
            )
        }

        val description = tryBuildDescription(manifest, version, errors)

        val hasErrors = errors.any { it.severity == ValidationSeverity.ERROR }
        return MetadataValidationResult(
            isValid = !hasErrors,
            description = description,
            errors = errors
        )
    }

    fun validateToDescription(manifest: ArcModuleManifest): ModuleDescription {
        val result = validate(manifest)
        if (!result.isValid) {
            val errorMessages = result.errors
                .filter { it.severity == ValidationSeverity.ERROR }
                .joinToString("; ") { "${it.field}: ${it.message}" }
            throw cc.arccore.loader.metadata.exception.MetadataValidationException(
                message = "Module manifest validation failed: $errorMessages",
                errors = result.errors
            )
        }
        return result.description
            ?: throw cc.arccore.loader.metadata.exception.MetadataValidationException(
                message = "Module manifest validation failed: description could not be built",
                errors = result.errors
            )
    }

    private fun tryBuildDescription(
        manifest: ArcModuleManifest,
        version: ModuleVersion?,
        errors: MutableList<MetadataValidationError>
    ): ModuleDescription? {
        if (manifest.id.isBlank() || manifest.main.isBlank()) return null

        val resolvedVersion = version ?: ModuleVersion(0, 0, 0)

        val dependencies = parseDependencies(manifest.depends, hard = true, errors)
        val softDependencies = parseDependencies(manifest.softDepends, hard = false, errors)

        val mergedAuthors = mergeAuthors(manifest.author, manifest.authors)

        val loadBefore = manifest.loadBefore.filter { id ->
            if (id == manifest.id) {
                errors.add(
                    MetadataValidationError(
                        "loadBefore",
                        "Module '$id' cannot specify itself in loadBefore",
                        ValidationSeverity.WARNING
                    )
                )
                false
            } else true
        }

        return ModuleDescription(
            id = manifest.id,
            name = manifest.name.ifBlank { manifest.id },
            version = resolvedVersion,
            description = manifest.description,
            authors = mergedAuthors,
            dependencies = dependencies,
            softDependencies = softDependencies,
            loadBefore = loadBefore,
            dependPlugins = manifest.dependPlugins,
            mainClass = manifest.main,
            apiVersion = manifest.apiVersion.ifBlank { "1.0" },
            website = manifest.website
        )
    }

    private fun parseDependencies(
        expressions: List<String>,
        hard: Boolean,
        errors: MutableList<MetadataValidationError>
    ): List<ModuleDependency> {
        return expressions.mapIndexed { index, expr ->
            try {
                ModuleDependency.parse(expr).copy(optional = if (hard) false else true)
            } catch (e: IllegalArgumentException) {
                errors.add(
                    MetadataValidationError(
                        if (hard) "depends[$index]" else "softDepends[$index]",
                        "Failed to parse dependency '$expr': ${e.message}",
                        ValidationSeverity.WARNING
                    )
                )
                ModuleDependency(
                    id = expr.split(":", limit = 2).first().trim(),
                    optional = !hard
                )
            }
        }
    }

    private fun mergeAuthors(author: String, authors: List<String>): List<String> {
        val result = mutableListOf<String>()
        if (author.isNotBlank()) result.add(author)
        for (a in authors) {
            if (a.isNotBlank() && a !in result) {
                result.add(a)
            }
        }
        return result
    }
}
