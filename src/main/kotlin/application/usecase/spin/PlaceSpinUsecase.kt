package application.usecase.spin

import application.event.SpinPlacedEvent
import application.port.outbound.EventPublisherAdapter
import application.service.GameService
import application.service.SessionService
import application.service.SpinCommand
import application.service.SpinService
import com.nekgamebling.application.service.AggregatorService
import domain.common.error.GameUnavailableError
import shared.value.SessionToken
import java.math.BigInteger

/**
 * Use case for placing a spin (making a bet).
 */
class PlaceSpinUsecase(
    private val sessionService: SessionService,
    private val gameService: GameService,
    private val spinService: SpinService,
    private val aggregatorService: AggregatorService,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(
        token: SessionToken,
        gameSymbol: String,
        extRoundId: String,
        transactionId: String,
        freeSpinId: String?,
        amount: BigInteger
    ): Result<Unit> {
        // Find session
        val session = sessionService.findByToken(token).getOrElse {
            return Result.failure(it)
        }

        //Find aggregator
        val aggregator = aggregatorService.findById(session.aggregatorId).getOrElse {
            return Result.failure(it)
        }

        // Find game
        val game = gameService.findBySymbol(symbol = gameSymbol, aggregator = aggregator.aggregator).getOrElse {
            return Result.failure(it)
        }

        // Validate game is active
        if (!game.isPlayable()) {
            return Result.failure(GameUnavailableError(gameSymbol))
        }

        // Create spin command
        val command = SpinCommand(
            extRoundId = extRoundId,
            transactionId = transactionId,
            amount = amount,
            freeSpinId = freeSpinId
        )

        // Place spin
        spinService.place(session, game, command).getOrElse {
            return Result.failure(it)
        }

        // Publish event
        eventPublisher.publish(
            SpinPlacedEvent(
                gameIdentity = game.identity,
                amount = amount,
                currency = session.currency,
                playerId = session.playerId,
                freeSpinId = freeSpinId
            )
        )

        return Result.success(Unit)
    }
}
