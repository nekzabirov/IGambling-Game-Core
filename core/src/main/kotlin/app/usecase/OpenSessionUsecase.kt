package app.usecase

import app.adapter.EventProducerAdapter
import app.event.SessionOpenEvent
import core.value.Currency
import core.value.Locale
import core.model.Platform
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import app.service.GameService
import app.service.SessionService
import domain.game.dao.full
import domain.game.mapper.toGameFull
import domain.game.table.GameTable
import domain.session.table.SessionTable
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

class OpenSessionUsecase : KoinComponent {
    private val eventProducer = getKoin().get<EventProducerAdapter>()

    suspend operator fun invoke(
        gameIdentity: String,
        playerId: String,
        currency: Currency,
        locale: Locale,
        platformN: Platform,
        lobbyUrl: String,
    ): Result<Response> = newSuspendedTransaction {
        val game = GameTable.full()
            .andWhere { GameTable.identity eq gameIdentity }
            .singleOrNull()?.toGameFull() ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

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

        val sessionId = SessionTable.insertAndGetId {
            it[SessionTable.gameId] = game.id
            it[SessionTable.playerId] = playerId
            it[SessionTable.token] = token
            it[SessionTable.currency] = currency.value
            it[SessionTable.locale] = locale.value
            it[SessionTable.platform] = platformN
            it[SessionTable.aggregatorId] = game.aggregator.id
        }.value

        eventProducer.publish(SessionOpenEvent(
            game = game,
            playerId = playerId,
            sessionId = sessionId
        ))

        Result.success(Response(result))
    }

    data class Response(val launchUrl: String)
}