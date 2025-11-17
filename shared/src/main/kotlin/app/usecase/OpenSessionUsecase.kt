package app.usecase

import core.value.Currency
import core.value.Locale
import core.model.Platform
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import app.service.GameService
import app.service.SessionService
import domain.session.table.SessionTable
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class OpenSessionUsecase {
    suspend operator fun invoke(
        gameIdentity: String,
        playerId: String,
        currency: Currency,
        locale: Locale,
        platformN: Platform,
        lobbyUrl: String,
    ): Result<Response> = newSuspendedTransaction {
        val game = GameService.findByIdentity(gameIdentity)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        if (game.locales.contains(locale).not())
            return@newSuspendedTransaction Result.failure(BadRequestException("Locale not supported"))

        if (game.platforms.contains(platformN).not())
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
            platform = platformN,
            isDemo = false
        )).getOrElse { return@newSuspendedTransaction Result.failure(it) }

        SessionTable.insert {
            it[SessionTable.gameId] = game.id
            it[SessionTable.playerId] = playerId
            it[SessionTable.token] = token
            it[SessionTable.currency] = currency.value
            it[SessionTable.locale] = locale.value
            it[SessionTable.platform] = platformN
            it[SessionTable.aggregatorId] = game.aggregator.id
        }

        Result.success(Response(result))
    }

    data class Response(val launchUrl: String)
}