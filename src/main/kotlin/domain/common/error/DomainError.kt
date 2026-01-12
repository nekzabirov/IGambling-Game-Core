package domain.common.error

/**
 * Base sealed class for all domain errors.
 * Using sealed class allows exhaustive when expressions and type-safe error handling.
 */
sealed class DomainError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /** Error code for external systems */
    abstract val code: String
}

/**
 * Entity was not found in the system.
 */
data class NotFoundError(
    val entity: String,
    val identifier: String,
    override val cause: Throwable? = null
) : DomainError("$entity not found: $identifier") {
    override val code: String = "NOT_FOUND"
}

/**
 * Validation failed for the given input.
 */
data class ValidationError(
    val field: String,
    val reason: String,
    override val cause: Throwable? = null
) : DomainError("Validation failed for $field: $reason") {
    override val code: String = "VALIDATION_ERROR"
}

/**
 * Player has insufficient balance for the operation.
 */
data class InsufficientBalanceError(
    val playerId: String,
    val required: Long,
    val available: Long,
    override val cause: Throwable? = null
) : DomainError("Insufficient balance for player $playerId: required $required, available $available") {
    override val code: String = "INSUFFICIENT_BALANCE"
}

/**
 * Bet amount exceeds the configured limit.
 */
data class BetLimitExceededError(
    val playerId: String,
    val betAmount: Long,
    val limit: Long,
    override val cause: Throwable? = null
) : DomainError("Bet limit exceeded for player $playerId: bet $betAmount, limit $limit") {
    override val code: String = "BET_LIMIT_EXCEEDED"
}

/**
 * Session is not valid or has expired.
 */
data class SessionInvalidError(
    val sessionToken: String,
    val reason: String = "Session is invalid or expired",
    override val cause: Throwable? = null
) : DomainError("$reason: $sessionToken") {
    override val code: String = "SESSION_INVALID"
}

/**
 * Game is not available for play.
 */
data class GameUnavailableError(
    val gameIdentity: String,
    val reason: String = "Game is not available",
    override val cause: Throwable? = null
) : DomainError("$reason: $gameIdentity") {
    override val code: String = "GAME_UNAVAILABLE"
}

/**
 * Round has already been finished and cannot be modified.
 */
data class RoundFinishedError(
    val roundId: String,
    override val cause: Throwable? = null
) : DomainError("Round already finished: $roundId") {
    override val code: String = "ROUND_FINISHED"
}

/**
 * Round was not found.
 */
data class RoundNotFoundError(
    val roundId: String,
    override val cause: Throwable? = null
) : DomainError("Round not found: $roundId") {
    override val code: String = "ROUND_NOT_FOUND"
}

/**
 * Preset configuration is invalid.
 */
data class InvalidPresetError(
    val presetId: String,
    val reason: String,
    override val cause: Throwable? = null
) : DomainError("Invalid preset $presetId: $reason") {
    override val code: String = "INVALID_PRESET"
}

/**
 * External service (aggregator, wallet, etc.) returned an error.
 */
data class ExternalServiceError(
    val service: String,
    val details: String,
    override val cause: Throwable? = null
) : DomainError("External service error from $service: $details") {
    override val code: String = "EXTERNAL_SERVICE_ERROR"
}

/**
 * Operation is not allowed in the current state.
 */
data class IllegalStateError(
    val operation: String,
    val currentState: String,
    override val cause: Throwable? = null
) : DomainError("Operation '$operation' not allowed in state: $currentState") {
    override val code: String = "ILLEGAL_STATE"
}

/**
 * Duplicate entity error.
 */
data class DuplicateEntityError(
    val entity: String,
    val identifier: String,
    override val cause: Throwable? = null
) : DomainError("$entity already exists: $identifier") {
    override val code: String = "DUPLICATE_ENTITY"
}

/**
 * Aggregator is not supported.
 */
data class AggregatorNotSupportedError(
    val aggregator: String,
    override val cause: Throwable? = null
) : DomainError("Aggregator not supported: $aggregator") {
    override val code: String = "AGGREGATOR_NOT_SUPPORTED"
}
