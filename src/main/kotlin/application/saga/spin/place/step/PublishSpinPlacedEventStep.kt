package application.saga.spin.place.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import domain.common.event.SpinPlacedEvent

/**
 * Step 6: Publish spin placed event.
 */
class PublishSpinPlacedEventStep(
    private val eventPublisher: EventPublisherAdapter
) : ValidationStep<PlaceSpinContext>("publish_event", "Publish Spin Placed Event") {

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val game = context.game ?: return Result.failure(
            IllegalStateError("publish_event", "game not set")
        )

        eventPublisher.publish(
            SpinPlacedEvent(
                gameIdentity = game.identity,
                amount = context.amount,
                currency = context.session.currency,
                playerId = context.session.playerId,
                freeSpinId = context.freeSpinId
            )
        )

        return Result.success(Unit)
    }
}
