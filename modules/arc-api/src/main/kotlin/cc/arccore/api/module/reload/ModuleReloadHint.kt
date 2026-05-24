package cc.arccore.api.module.reload

/**
 * 향후 hot-reload 시 모듈이 자신의 상태를 직렬화/역직렬화할 수 있는 확장점.
 * 현재는 인터페이스 정의만 제공하며 실제 reload 구현은 불필요하다.
 */
sealed interface ModuleReloadHint {

    /**
     * 모듈이 상태를 직렬화 가능한 형태로 제공할 수 있음을 나타낸다.
     * reload 전 [captureState]를 호출하고, 신규 인스턴스 로드 후 [restoreState]를 호출한다.
     */
    interface StatefulReload : ModuleReloadHint {
        fun captureState(): Map<String, Any?>
        fun restoreState(state: Map<String, Any?>)
    }

    /**
     * 모듈이 상태를 보존하지 않는 stateless reload를 선호함을 나타낸다.
     * 프레임워크는 단순히 unload → load를 수행한다.
     */
    interface StatelessReload : ModuleReloadHint

    /**
     * 모듈이 특정 조건에서만 reload를 허용함을 나타낸다.
     */
    interface ConditionalReload : ModuleReloadHint {
        fun canReload(): Boolean
        fun rejectReason(): String
    }
}
