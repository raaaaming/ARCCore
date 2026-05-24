package cc.arccore.api.command.exception

class CommandNotFoundException(val commandName: String) :
    CommandRegistrationException("Command '$commandName' is not registered")
