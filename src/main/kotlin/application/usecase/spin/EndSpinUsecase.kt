package application.usecase.spin

import domain.common.event.SpinEndEvent
import application.port.outbound.EventPublisherAdapter
import application.service.GameService
import application.service.SpinService
import domain.session.model.Session

/**
 * Use case for ending/closing a spin round.
 * Accepts pre-resolved Session to avoid duplicate lookups.
 */
class EndSpinUsecase(
    private val spinService: SpinService,
    private val gameService: GameService,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(
        session: Session,
        extRoundId: String,
        freeSpinId: String?
    ): Result<Unit> {
        val game = gameService.findById(session.gameId).getOrElse {
            return Result.failure(it)
        }

        spinService.closeRound(session, extRoundId).getOrElse {
            return Result.failure(it)
        }

        eventPublisher.publish(
            SpinEndEvent(
                gameIdentity = game.identity,
                playerId = session.playerId,
                freeSpinId = freeSpinId,
            )
        )

        return Result.success(Unit)
    }
}
