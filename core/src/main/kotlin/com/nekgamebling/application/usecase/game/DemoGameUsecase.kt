package com.nekgamebling.application.usecase.game

import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.application.service.GameService
import com.nekgamebling.domain.common.error.AggregatorNotSupportedError
import com.nekgamebling.domain.common.error.ValidationError
import com.nekgamebling.shared.value.Currency
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform

/**
 * Result of demo game operation.
 */
data class DemoGameResult(
    val launchUrl: String
)

/**
 * Use case for launching a game in demo mode.
 */
class DemoGameUsecase(
    private val gameService: GameService,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {
    suspend operator fun invoke(
        gameIdentity: String,
        currency: Currency,
        locale: Locale,
        platform: Platform,
        lobbyUrl: String
    ): Result<DemoGameResult> {
        val game = gameService.findByIdentity(gameIdentity).getOrElse {
            return Result.failure(it)
        }

        // Validate locale support
        if (!game.supportsLocale(locale)) {
            return Result.failure(
                ValidationError("locale", "Game does not support locale: ${locale.value}")
            )
        }

        // Validate platform support
        if (!game.supportsPlatform(platform)) {
            return Result.failure(
                ValidationError("platform", "Game does not support platform: $platform")
            )
        }

        // Get aggregator adapter
        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val launchUrlAdapter = factory.createLaunchUrlAdapter(game.aggregator)

        // Get demo launch URL
        val launchUrl = launchUrlAdapter.getLaunchUrl(
            gameSymbol = game.symbol,
            sessionToken = "demo",
            locale = locale,
            platform = platform,
            lobbyUrl = lobbyUrl,
            playerId = "demo",
            currency = currency,
            demo = true
        ).getOrElse {
            return Result.failure(it)
        }

        return Result.success(DemoGameResult(launchUrl))
    }
}
