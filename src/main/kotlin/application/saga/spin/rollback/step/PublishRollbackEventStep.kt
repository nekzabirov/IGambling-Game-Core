package application.saga.spin.rollback.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import application.service.GameService
import domain.common.event.SpinRollbackEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Step 4: Publish rollback event (fire-and-forget for speed).
 */
class PublishRollbackEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<RollbackSpinContext>("publish_rollback_event", "Publish Rollback Event") {

    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        // Capture values needed for async block
        val refundAmount = context.refundRealAmount + context.refundBonusAmount
        val freeSpinId = context.originalSpin?.freeSpinId

        // Fire-and-forget: publish event asynchronously (includes game lookup)
        scope.launch {
            val gameIdentity = gameService.findById(context.session.gameId)
                .map { it.identity }
                .getOrElse { "unknown" }

            context.gameIdentity = gameIdentity

            eventPublisher.publish(
                SpinRollbackEvent(
                    gameIdentity = gameIdentity,
                    playerId = context.session.playerId,
                    refundAmount = refundAmount,
                    currency = context.session.currency,
                    freeSpinId = freeSpinId
                )
            )
        }

        return Result.success(Unit)
    }
}
