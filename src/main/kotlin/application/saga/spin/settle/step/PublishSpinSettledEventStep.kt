package application.saga.spin.settle.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import application.service.GameService
import domain.common.event.SpinSettledEvent

/**
 * Step 6: Publish spin settled event.
 */
class PublishSpinSettledEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<SettleSpinContext>("publish_settled_event", "Publish Spin Settled Event") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        // Get game identity for the event
        val game = gameService.findById(context.session.gameId).getOrElse {
            // Non-critical: log warning but don't fail
            context.gameIdentity = "unknown"
            return Result.success(Unit)
        }

        context.gameIdentity = game.identity

        eventPublisher.publish(
            SpinSettledEvent(
                gameIdentity = game.identity,
                amount = context.winAmount,
                currency = context.session.currency,
                playerId = context.session.playerId,
                freeSpinId = context.freeSpinId
            )
        )

        return Result.success(Unit)
    }
}
