package application.saga.spin.settle.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import application.service.GameService
import domain.common.event.SpinSettledEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Step 5: Publish spin settled event (fire-and-forget for speed).
 */
class PublishSpinSettledEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<SettleSpinContext>("publish_settled_event", "Publish Spin Settled Event") {

    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        // Fire-and-forget: publish event asynchronously (includes game lookup)
        scope.launch {
            val gameIdentity = gameService.findById(context.session.gameId)
                .map { it.identity }
                .getOrElse { "unknown" }

            context.gameIdentity = gameIdentity

            eventPublisher.publish(
                SpinSettledEvent(
                    gameIdentity = gameIdentity,
                    amount = context.winAmount,
                    currency = context.session.currency,
                    playerId = context.session.playerId,
                    freeSpinId = context.freeSpinId
                )
            )
        }

        return Result.success(Unit)
    }
}
