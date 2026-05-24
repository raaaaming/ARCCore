package cc.arccore.api.command.exception

class DuplicateCommandException(
    val commandName: String,
    val existingOwnerId: String
) : CommandRegistrationException(
    "Command '$commandName' is already registered by module '$existingOwnerId'"
)
