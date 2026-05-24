package cc.arccore.api.di

sealed interface Scope {
    object Singleton : Scope
    object Module : Scope
    object Transient : Scope
}
