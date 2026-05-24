package cc.arccore.ksp.model

/**
 * Data collected by KSP for a single @ArcComponent class with an injectable constructor.
 *
 * [constructorParams] lists the fully-qualified type names of each constructor
 * parameter in declaration order. [FactoryGenerator] uses this to emit the
 * `context.resolve(Type::class)` calls that replace runtime reflection.
 */
data class InjectableEntry(
    val className: String,
    val factoryClassName: String,
    val scope: InjectableScope,
    val constructorParams: List<ParamEntry>
) {
    data class ParamEntry(
        val name: String,
        val typeFqcn: String
    )

    enum class InjectableScope { SINGLETON, MODULE, TRANSIENT }

    val packageName: String get() = className.substringBeforeLast('.', missingDelimiterValue = "")
    val simpleName: String get() = className.substringAfterLast('.')
}
