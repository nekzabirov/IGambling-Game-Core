package application.saga.spin.end.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.end.EndSpinContext
import application.service.GameService
import domain.common.event.SpinEndEvent

/**
 * Step 3: Publish spin end event.
 */
class PublishSpinEndEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<EndSpinContext>("publish_end_event", "Publish Spin End Event") {

    override suspend fun execute(context: EndSpinContext): Result<Unit> {
        val game = gameService.findById(context.session.gameId).getOrElse {
            // Non-critical: log warning but don't fail
            context.gameIdentity = "unknown"
            return Result.success(Unit)
        }

        context.gameIdentity = game.identity

        eventPublisher.publish(
            SpinEndEvent(
                gameIdentity = game.identity,
                playerId = context.session.playerId,
                freeSpinId = context.freeSpinId
            )
        )

        return Result.success(Unit)
    }
}
