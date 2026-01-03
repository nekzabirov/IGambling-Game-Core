package application.saga.spin.end

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.RoundRepository
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.end.step.*
import application.service.GameService

/**
 * Saga definition for ending/closing a spin round.
 *
 * Step order:
 * 1. FindRound - find the round by external ID
 * 2. CloseRound - mark round as finished
 * 3. PublishEvent - publish spin end event
 */
class EndSpinSaga(
    private val gameService: GameService,
    private val eventPublisher: EventPublisherAdapter,
    private val roundRepository: RoundRepository
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "EndSpinSaga",
        steps = listOf(
            FindRoundStep(roundRepository),
            CloseRoundStep(roundRepository),
            PublishSpinEndEventStep(eventPublisher, gameService)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: EndSpinContext): Result<Unit> {
        return orchestrator.execute(context)
    }
}
