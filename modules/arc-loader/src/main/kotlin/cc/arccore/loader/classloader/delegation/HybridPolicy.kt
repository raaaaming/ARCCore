package cc.arccore.loader.classloader.delegation

class HybridPolicy(
    private val isSharedPackage: (String) -> Boolean
) : DelegationPolicy {

    private val parentFirst = ParentFirstPolicy()
    private val childFirst = ChildFirstPolicy()

    override fun resolveClass(context: DelegationContext): DelegationResult {
        return if (isSharedPackage(context.className)) {
            parentFirst.resolveClass(context)
        } else {
            childFirst.resolveClass(context)
        }
    }
}
