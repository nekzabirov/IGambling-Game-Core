package application.saga.spin.end.step

import application.port.outbound.EventPublisherAdapter
import application.saga.ValidationStep
import application.saga.spin.end.EndSpinContext
import application.service.GameService
import domain.common.event.SpinEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Step 3: Publish spin end event (fire-and-forget for speed).
 */
class PublishSpinEndEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<EndSpinContext>("publish_end_event", "Publish Spin End Event") {

    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun execute(context: EndSpinContext): Result<Unit> {
        // Fire-and-forget: publish event asynchronously (includes game lookup)
        scope.launch {
            val gameIdentity = gameService.findById(context.session.gameId)
                .map { it.identity }
                .getOrElse { "unknown" }

            context.gameIdentity = gameIdentity

            eventPublisher.publish(
                SpinEndEvent(
                    gameIdentity = gameIdentity,
                    playerId = context.session.playerId,
                    freeSpinId = context.freeSpinId
                )
            )
        }

        return Result.success(Unit)
    }
}
