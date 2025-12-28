package application.saga.spin.rollback.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import application.service.GameService
import domain.common.event.SpinRollbackEvent

/**
 * Step 5: Publish rollback event.
 */
class PublishRollbackEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<RollbackSpinContext>("publish_rollback_event", "Publish Rollback Event") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val game = gameService.findById(context.session.gameId).getOrElse {
            // Non-critical: log warning but don't fail
            context.gameIdentity = "unknown"
            return Result.success(Unit)
        }

        context.gameIdentity = game.identity

        eventPublisher.publish(
            SpinRollbackEvent(
                gameIdentity = game.identity,
                playerId = context.session.playerId,
                refundAmount = context.refundRealAmount + context.refundBonusAmount,
                currency = context.session.currency,
                freeSpinId = context.originalSpin?.freeSpinId
            )
        )

        return Result.success(Unit)
    }
}
