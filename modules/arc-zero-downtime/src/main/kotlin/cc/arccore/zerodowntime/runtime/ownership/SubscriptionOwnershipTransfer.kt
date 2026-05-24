package cc.arccore.zerodowntime.runtime.ownership

import cc.arccore.zerodowntime.runtime.model.TransitionContext
import java.util.concurrent.ConcurrentHashMap

internal class SubscriptionOwnershipTransfer {
    private val pendingCancelModules: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun pauseOldSubscriptions(moduleId: String) {
        pendingCancelModules.add(moduleId)
    }

    fun finalizeCancellation(moduleId: String): SubscriptionTransferResult {
        val wasPending = pendingCancelModules.remove(moduleId)
        return if (wasPending) {
            SubscriptionTransferResult.Success(0)
        } else {
            SubscriptionTransferResult.Skipped("Module $moduleId was not in pending cancel state")
        }
    }

    fun transfer(
        oldModuleId: String,
        newModuleId: String,
        context: TransitionContext
    ): SubscriptionTransferResult {
        pauseOldSubscriptions(oldModuleId)
        return SubscriptionTransferResult.Success(0)
    }
}
