package usecase

import core.value.Currency
import core.value.Locale
import core.value.Platform
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import domain.game.service.GameService
import domain.session.service.SessionService
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class OpenSessionUsecase {
    suspend operator fun invoke(
        gameIdentity: String,
        playerId: String,
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

        val token = SessionService.generateSessionToken()

        val result = adapter.createLaunchUrl(CreateLaunchUrlCommand(
            gameSymbol = game.symbol,
            playerId = playerId,
            sessionToken = token,
            lobbyUrl = lobbyUrl,
            locale = locale,
            currency = currency,
            platform = platform,
            isDemo = false
        )).getOrElse { return@newSuspendedTransaction Result.failure(it) }

        Result.success(Response(result))
    }

    data class Response(val launchUrl: String)
}