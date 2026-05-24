package cc.arccore.runtime

@Deprecated(
    message = "Use cc.arccore.runtime.lifecycle.ModuleRuntime from the lifecycle sub-package",
    replaceWith = ReplaceWith(
        "cc.arccore.runtime.lifecycle.ModuleRuntime",
        "cc.arccore.runtime.lifecycle.ModuleRuntime"
    ),
    level = DeprecationLevel.WARNING
)
typealias ModuleRuntime = cc.arccore.runtime.lifecycle.ModuleRuntime
