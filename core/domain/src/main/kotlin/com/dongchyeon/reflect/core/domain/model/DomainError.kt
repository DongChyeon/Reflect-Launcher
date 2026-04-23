package com.dongchyeon.reflect.core.domain.model

sealed class DomainError : Throwable() {
    data class Storage(override val message: String?) : DomainError()
    data class Permission(override val message: String?) : DomainError()
    data class Business(override val message: String?) : DomainError()

    companion object {
        fun from(throwable: Throwable): DomainError = when (throwable) {
            is DomainError -> throwable
            is SecurityException -> Permission(throwable.message)
            else -> Storage(throwable.message)
        }
    }
}
