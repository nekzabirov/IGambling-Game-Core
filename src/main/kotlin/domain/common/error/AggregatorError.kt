package domain.common.error

class AggregatorError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
    override val errorCode: ErrorCode = ErrorCode.AGGREGATOR_ERROR
}