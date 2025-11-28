package com.nekgamebling.domain.common.error

class AggregatorError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
    override val code: String = message
}