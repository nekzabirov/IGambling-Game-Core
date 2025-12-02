package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.model.AggregatorInfo
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import shared.value.Currency
import shared.value.Locale
import shared.value.Platform

/**
 * Pragmatic implementation for getting game launch URLs.
 */
class PragmaticLaunchUrlAdapter(
    private val aggregatorInfo: AggregatorInfo
) : AggregatorLaunchUrlPort {

    private val config = PragmaticConfig(aggregatorInfo.config)
    private val client = PragmaticHttpClient(config)

    override suspend fun getLaunchUrl(
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
        demo: Boolean
    ): Result<String> {
        TODO("Not implemented: Get launch URL from Pragmatic")
    }
}
