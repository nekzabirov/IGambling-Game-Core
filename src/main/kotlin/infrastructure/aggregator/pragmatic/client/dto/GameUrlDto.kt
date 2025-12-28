package infrastructure.aggregator.pragmatic.client.dto

import domain.common.value.Platform

/**
 * DTO for requesting game launch URL from Pragmatic Play API.
 * Used as payload for POST /IntegrationService/v3/http/CasinoGameAPI/game/url/
 */
data class LaunchUrlRequestDto(
    val gameSymbol: String,
    val sessionToken: String,
    val playerId: String,
    val locale: String,
    val platform: Platform,
    val currency: String,
    val lobbyUrl: String,
    val demo: Boolean
)
