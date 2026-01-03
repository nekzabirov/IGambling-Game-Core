package application.saga.spin.rollback

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.rollback.step.*
import application.service.GameService

/**
 * Saga definition for rolling back a spin (refunding a bet).
 *
 * Step order:
 * 1. FindRound - find the round by external ID
 * 2. FindOriginalSpin - find the spin to rollback
 * 3. WalletRefund - refund the bet amount to wallet
 * 4. SaveRollbackSpin - save rollback spin record
 * 5. PublishEvent - publish rollback event
 */
class RollbackSpinSaga(
    private val gameService: GameService,
    private val walletAdapter: WalletAdapter,
    private val eventPublisher: EventPublisherAdapter
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "RollbackSpinSaga",
        steps = listOf(
            FindRoundStep(),
            FindOriginalSpinStep(),
            WalletRefundStep(walletAdapter),
            SaveRollbackSpinStep(),
            PublishRollbackEventStep(eventPublisher, gameService)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        return orchestrator.execute(context)
    }
}
