package infrastructure.aggregator.pragmatic.client.dto

/**
 * DTO for creating free spins in Pragmatic Play API.
 * Used as payload for POST /IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/player/create
 */
data class CreateFreespinDto(
    val bonusCode: String,
    val playerId: String,
    val gameSymbol: String,
    val currency: String,
    val rounds: Int,
    val totalBet: Double,
    val startTimestamp: Long,
    val expirationTimestamp: Long
)
