package cc.arccore.api.command

sealed class CommandResult {
    data object Success : CommandResult()
    data class Failure(val message: String = "") : CommandResult()
    data class NoPermission(val message: String = "You don't have permission to use this command.") : CommandResult()
    data class InvalidUsage(val usage: String) : CommandResult()
    data object NotFound : CommandResult()

    val isSuccess: Boolean get() = this is Success

    /**
     * 커맨드 실행기가 "처리 완료(true)" / "usage 출력 필요(false)"를 반환해야 하는 경우를 위한
     * Boolean 변환. Bukkit CommandExecutor 어댑터에서 사용한다.
     */
    fun toHandled(): Boolean = when (this) {
        is Success -> true
        is NoPermission -> true
        is InvalidUsage -> false
        is Failure -> false
        is NotFound -> false
    }

    /**
     * Bukkit CommandExecutor 어댑터 호환성을 위한 별칭.
     * @see toHandled
     */
    @Deprecated(
        message = "메서드명에 Bukkit이 포함된 arc-api 규칙 위반. toHandled()를 사용하라.",
        replaceWith = ReplaceWith("toHandled()"),
        level = DeprecationLevel.WARNING
    )
    fun toBukkitBoolean(): Boolean = toHandled()
}
