package app.usecase

import core.value.Currency
import core.value.Locale
import core.model.Platform
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import app.service.GameService
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class DemoGameUsecase {
    suspend operator fun invoke(
        gameIdentity: String,
        currency: Currency,
        locale: Locale,
        platform: Platform,
        lobbyUrl: String,
    ): Result<Response> = newSuspendedTransaction {
        val game = GameService.findByIdentity(gameIdentity)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        if (game.locales.contains(locale).not())
            return@newSuspendedTransaction Result.failure(BadRequestException("Locale not supported"))

        if (game.platforms.contains(platform).not())
            return@newSuspendedTransaction Result.failure(BadRequestException("Platform not supported"))

        val adapter = AggregatorFabric.createAdapter(game.aggregator.config, game.aggregator.aggregator)

        val result = adapter.createLaunchUrl(CreateLaunchUrlCommand(
            gameSymbol = game.symbol,
            lobbyUrl = lobbyUrl,
            locale = locale,
            currency = currency,
            platform = platform,
            isDemo = true
        )).getOrElse { return@newSuspendedTransaction Result.failure(it) }

        Result.success(Response(result))
    }

    data class Response(val launchUrl: String)
}