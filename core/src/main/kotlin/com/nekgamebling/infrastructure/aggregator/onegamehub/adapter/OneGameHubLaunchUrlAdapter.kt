package com.nekgamebling.infrastructure.aggregator.onegamehub.adapter

import com.nekgamebling.application.port.outbound.AggregatorLaunchUrlPort
import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.common.error.AggregatorError
import com.nekgamebling.infrastructure.aggregator.onegamehub.OneGameHubConfig
import com.nekgamebling.infrastructure.aggregator.onegamehub.client.OneGameHubHttpClient
import com.nekgamebling.shared.value.Currency
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform

/**
 * OneGameHub implementation for getting game launch URLs.
 */
class OneGameHubLaunchUrlAdapter(
    private val aggregatorInfo: AggregatorInfo
) : AggregatorLaunchUrlPort {

    private val config = OneGameHubConfig(aggregatorInfo.config)
    private val client = OneGameHubHttpClient(config)

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
        val response = client.getLaunchUrl(
            gameSymbol = gameSymbol,
            sessionToken = sessionToken,
            playerId = playerId,
            locale = locale.value,
            platform = platform,
            currency = currency.value,
            lobbyUrl = lobbyUrl,
            demo = demo
        ).getOrElse {
            return Result.failure(it)
        }

        if (!response.success) {
            return Result.failure(AggregatorError("Error to get launch url status: ${response.status}"))
        }

        return Result.success(response.response!!.gameUrl)
    }
}
