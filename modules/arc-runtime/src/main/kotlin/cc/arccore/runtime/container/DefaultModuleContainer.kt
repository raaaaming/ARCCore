package cc.arccore.runtime.container

import cc.arccore.api.exception.ModuleStateException
import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.api.module.ModuleContext
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleState
import java.util.concurrent.atomic.AtomicReference

/**
 * ModuleContainerView의 스레드 안전 구현체.
 * 상태 전이는 AtomicReference CAS로 원자화됩니다.
 */
class DefaultModuleContainer(
    override val module: ArcModuleAPI
) : ModuleContainerView {

    private val _state = AtomicReference(ModuleState.CREATED)
    private val _failureCause = AtomicReference<Throwable?>(null)
    private val _context = AtomicReference<ModuleContext?>(null)

    override val state: ModuleState get() = _state.get()
    override val failureCause: Throwable? get() = _failureCause.get()
    override val context: ModuleContext? get() = _context.get()

    /**
     * 원자적 상태 전이. 전이 불가능한 경우 ModuleStateException 발생.
     * CAS 경쟁에서 진 경우 false를 반환하며, 호출자가 재시도 여부를 결정합니다.
     */
    fun transitionTo(newState: ModuleState, error: Throwable? = null): Boolean {
        val current = _state.get()
        if (!current.canTransitionTo(newState)) {
            throw ModuleStateException(
                "Cannot transition module '${module.id}' from $current to $newState"
            )
        }
        val success = _state.compareAndSet(current, newState)
        if (success && newState == ModuleState.FAILED) {
            _failureCause.set(error)
        }
        return success
    }

    fun transitionToLoad(context: ModuleContext): Boolean {
        val success = transitionTo(ModuleState.LOADED)
        if (success) _context.set(context)
        return success
    }

    fun transitionToUnloaded(): Boolean {
        val success = transitionTo(ModuleState.UNLOADED)
        if (success) _context.set(null)
        return success
    }

    fun transitionToFailed(error: Throwable? = null): Boolean =
        transitionTo(ModuleState.FAILED, error)

    override fun toString(): String =
        "DefaultModuleContainer(module=${module.id}, state=${_state.get()})"
}
