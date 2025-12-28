package application.usecase.spin

import domain.common.event.SpinPlacedEvent
import application.port.outbound.EventPublisherAdapter
import application.service.GameService
import application.service.SpinCommand
import application.service.SpinService
import application.service.AggregatorService
import domain.common.error.GameUnavailableError
import domain.session.model.Session
import java.math.BigInteger

/**
 * Use case for placing a spin (making a bet).
 * Accepts pre-resolved Session to avoid duplicate lookups.
 */
class PlaceSpinUsecase(
    private val gameService: GameService,
    private val spinService: SpinService,
    private val aggregatorService: AggregatorService,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(
        session: Session,
        gameSymbol: String,
        extRoundId: String,
        transactionId: String,
        freeSpinId: String?,
        amount: BigInteger
    ): Result<Unit> {
        val aggregator = aggregatorService.findById(session.aggregatorId).getOrElse {
            return Result.failure(it)
        }

        val game = gameService.findBySymbol(symbol = gameSymbol, aggregator = aggregator.aggregator).getOrElse {
            return Result.failure(it)
        }

        if (!game.isPlayable()) {
            return Result.failure(GameUnavailableError(gameSymbol))
        }

        val command = SpinCommand(
            extRoundId = extRoundId,
            transactionId = transactionId,
            amount = amount,
            freeSpinId = freeSpinId
        )

        spinService.place(session, game, command).getOrElse {
            return Result.failure(it)
        }

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
