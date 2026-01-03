package application.saga.spin.place.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import domain.common.event.SpinPlacedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Step 6: Publish spin placed event (fire-and-forget for speed).
 */
class PublishSpinPlacedEventStep(
    private val eventPublisher: EventPublisherAdapter
) : ValidationStep<PlaceSpinContext>("publish_event", "Publish Spin Placed Event") {

    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val game = context.game ?: return Result.failure(
            IllegalStateError("publish_event", "game not set")
        )

        // Fire-and-forget: publish event asynchronously
        scope.launch {
            eventPublisher.publish(
                SpinPlacedEvent(
                    gameIdentity = game.identity,
                    amount = context.amount,
                    currency = context.session.currency,
                    playerId = context.session.playerId,
                    freeSpinId = context.freeSpinId
                )
            )
        }

        return Result.success(Unit)
    }
}
