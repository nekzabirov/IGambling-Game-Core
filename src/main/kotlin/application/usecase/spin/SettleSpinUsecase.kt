package application.usecase.spin

import domain.common.event.SpinSettledEvent
import application.port.outbound.EventPublisherAdapter
import application.service.GameService
import application.service.SpinCommand
import application.service.SpinService
import domain.session.model.Session
import java.math.BigInteger

/**
 * Use case for settling a spin (recording win/loss).
 * Accepts pre-resolved Session to avoid duplicate lookups.
 */
class SettleSpinUsecase(
    private val spinService: SpinService,
    private val gameService: GameService,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(
        session: Session,
        extRoundId: String,
        transactionId: String,
        freeSpinId: String?,
        winAmount: BigInteger
    ): Result<Unit> {
        if (winAmount <= BigInteger.ZERO) {
            return Result.success(Unit)
        }

        val command = SpinCommand(
            extRoundId = extRoundId,
            transactionId = transactionId,
            amount = winAmount,
            freeSpinId = freeSpinId
        )

        val game = gameService.findById(session.gameId).getOrElse {
            return Result.failure(it)
        }

        spinService.settle(session, extRoundId, command).getOrElse {
            return Result.failure(it)
        }

        eventPublisher.publish(
            SpinSettledEvent(
                gameIdentity = game.identity,
                amount = winAmount,
                currency = session.currency,
                playerId = session.playerId,
                freeSpinId = freeSpinId,
            )
        )

        return Result.success(Unit)
    }
}
