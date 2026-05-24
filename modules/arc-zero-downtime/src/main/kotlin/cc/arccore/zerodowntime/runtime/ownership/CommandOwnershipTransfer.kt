package cc.arccore.zerodowntime.runtime.ownership

import cc.arccore.zerodowntime.runtime.model.TransitionContext

internal class CommandOwnershipTransfer {
    fun transfer(
        oldModuleId: String,
        newModuleId: String,
        context: TransitionContext
    ): CommandTransferResult {
        // Commands는 stateless이므로 NEW 모듈 onEnable() 시 자동 재등록됨
        // OLD commands는 CleanupOldStage에서 제거됨
        return CommandTransferResult.Skipped("Command transfer handled by lifecycle pipeline")
    }
}
