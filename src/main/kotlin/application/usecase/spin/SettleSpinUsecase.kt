package application.usecase.spin

import application.event.SpinSettledEvent
import application.port.outbound.EventPublisherPort
import application.service.GameService
import application.service.SessionService
import application.service.SpinCommand
import application.service.SpinService
import shared.value.SessionToken

/**
 * Use case for settling a spin (recording win/loss).
 */
class SettleSpinUsecase(
    private val sessionService: SessionService,
    private val spinService: SpinService,
    private val gameService: GameService,
    private val eventPublisher: EventPublisherPort
) {
    suspend operator fun invoke(
        token: SessionToken,
        extRoundId: String,
        transactionId: String,
        freeSpinId: String?,
        winAmount: Int
    ): Result<Unit> {
        // Find session
        val session = sessionService.findByToken(token).getOrElse {
            return Result.failure(it)
        }

        // Create settle command
        val command = SpinCommand(
            extRoundId = extRoundId,
            transactionId = transactionId,
            amount = winAmount,
            freeSpinId = freeSpinId
        )

        // Settle spin
        spinService.settle(session, extRoundId, command).getOrElse {
            return Result.failure(it)
        }

        val game = gameService.findById(session.gameId).getOrElse {
            return Result.failure(it)
        }

        // Publish event
        eventPublisher.publish(
            SpinSettledEvent(
                gameIdentity = game.identity,
                amount = winAmount,
                currency = session.currency,
                playerId = session.playerId,
                freeSpinId = freeSpinId,
                winAmount = winAmount
            )
        )

        return Result.success(Unit)
    }
}
