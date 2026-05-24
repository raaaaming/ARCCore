package cc.arccore.api.module

/**
 * 상태 업데이트가 가능한 ModuleContext.
 * 라이프사이클 관리자가 구체 타입(SimpleModuleContext)에 결합되지 않도록
 * arc-runtime 내부에서 이 인터페이스를 통해 상태를 동기화합니다.
 */
interface MutableModuleContext : ModuleContext {
    fun updateState(newState: ModuleState)
}
