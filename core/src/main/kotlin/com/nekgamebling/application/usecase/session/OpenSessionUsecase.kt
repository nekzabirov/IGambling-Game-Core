package com.nekgamebling.application.usecase.session

import com.nekgamebling.application.event.SessionOpenedEvent
import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.application.port.outbound.EventPublisherPort
import com.nekgamebling.application.service.GameService
import com.nekgamebling.application.service.SessionService
import com.nekgamebling.domain.common.error.AggregatorNotSupportedError
import com.nekgamebling.domain.common.error.ValidationError
import com.nekgamebling.domain.session.model.Session
import com.nekgamebling.shared.value.Currency
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform
import java.util.UUID

/**
 * Command for opening a new session.
 */
data class OpenSessionCommand(
    val gameIdentity: String,
    val playerId: String,
    val currency: Currency,
    val locale: Locale,
    val platform: Platform,
    val loggyUrl: String,
)

/**
 * Result of opening a session.
 */
data class OpenSessionResult(
    val session: Session,
    val launchUrl: String
)

/**
 * Use case for opening a new game session.
 */
class OpenSessionUsecase(
    private val gameService: GameService,
    private val sessionService: SessionService,
    private val eventPublisher: EventPublisherPort,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {
    suspend operator fun invoke(command: OpenSessionCommand): Result<OpenSessionResult> {
        // Find game with details
        val game = gameService.findByIdentity(command.gameIdentity).getOrElse {
            return Result.failure(it)
        }

        // Validate locale support
        if (!game.supportsLocale(command.locale)) {
            return Result.failure(
                ValidationError("locale", "Game does not support locale: ${command.locale.value}")
            )
        }

        // Validate platform support
        if (!game.supportsPlatform(command.platform)) {
            return Result.failure(
                ValidationError("platform", "Game does not support platform: ${command.platform}")
            )
        }

        // Get aggregator adapter
        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val launchUrlAdapter = factory.createLaunchUrlAdapter(game.aggregator)

        // Generate session token
        val token = sessionService.generateSessionToken()

        // Create session
        val session = Session(
            id = UUID.randomUUID(),
            gameId = game.id,
            aggregatorId = game.aggregator.id,
            playerId = command.playerId,
            token = token,
            externalToken = null,
            currency = command.currency,
            locale = command.locale,
            platform = command.platform
        )

        // Save session
        val savedSession = sessionService.createSession(session).getOrElse {
            return Result.failure(it)
        }

        // Get launch URL from aggregator
        val launchUrl = launchUrlAdapter.getLaunchUrl(
            gameSymbol = game.symbol,
            sessionToken = token,
            playerId = command.playerId,
            locale = command.locale,
            platform = command.platform,
            currency = command.currency,
            lobbyUrl = command.loggyUrl,
            demo = false
        ).getOrElse {
            return Result.failure(it)
        }

        // Publish event
        eventPublisher.publish(
            SessionOpenedEvent(
                sessionId = savedSession.id.toString(),
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = command.playerId,
                currency = command.currency,
                platform = command.platform.name
            )
        )

        return Result.success(OpenSessionResult(savedSession, launchUrl))
    }
}
