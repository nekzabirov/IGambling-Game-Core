package infrastructure.aggregator.pateplay.client.dto

import kotlinx.serialization.Serializable

/**
 * DTO for creating free spins in PatePlay API.
 * Used as payload for POST /bonuses/create
 */
data class CreateFreespinRequestDto(
    val referenceId: String,
    val playerId: String,
    val currency: String,
    val ttlSeconds: Long,
    val gameSymbol: String,
    val stake: String,
    val rounds: Int,
    val expiresAt: String
)

/**
 * Serializable request body for /bonuses/create endpoint.
 */
@Serializable
data class CreateFreespinBodyDto(
    val bonuses: List<FreespinBonusDto>
)

@Serializable
data class FreespinBonusDto(
    val bonusRef: String,
    val playerId: String,
    val siteCode: String,
    val currency: String,
    val type: String = "bets",
    val config: FreespinConfigDto,
    val timeExpires: String
)

@Serializable
data class FreespinConfigDto(
    val ttl: Long,
    val games: List<String>,
    val stake: String,
    val bets: Int
)
