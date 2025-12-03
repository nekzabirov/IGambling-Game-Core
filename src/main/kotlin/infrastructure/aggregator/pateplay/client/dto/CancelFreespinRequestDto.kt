package infrastructure.aggregator.pateplay.client.dto

import kotlinx.serialization.Serializable

/**
 * DTO for cancelling free spins in PatePlay API.
 * Used as payload for POST /bonuses/cancel
 */
data class CancelFreespinRequestDto(
    val bonusId: Long,
    val reason: String = "Bonus cancelled by operator",
    val force: Boolean = false
)

/**
 * Serializable request body for /bonuses/cancel endpoint.
 */
@Serializable
data class CancelFreespinBodyDto(
    val ids: List<Long>,
    val reason: String,
    val force: Boolean
)
