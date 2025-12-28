package application.saga.spin.settle

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.settle.step.*
import application.service.GameService
import domain.session.repository.RoundRepository
import domain.session.repository.SpinRepository
import java.math.BigInteger

/**
 * Saga definition for settling a spin (determining win/loss).
 *
 * Step order:
 * 1. FindRound - find the round by external ID
 * 2. FindPlaceSpin - find the original place spin
 * 3. CalculateWinAmounts - determine real/bonus split
 * 4. WalletDeposit - deposit winnings (BEFORE saving)
 * 5. SaveSettleSpin - save settle spin record (AFTER wallet success)
 * 6. PublishEvent - publish domain event
 */
class SettleSpinSaga(
    private val gameService: GameService,
    private val walletAdapter: WalletAdapter,
    private val roundRepository: RoundRepository,
    private val spinRepository: SpinRepository,
    private val eventPublisher: EventPublisherAdapter
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "SettleSpinSaga",
        steps = listOf(
            FindRoundStep(roundRepository),
            FindPlaceSpinStep(spinRepository),
            CalculateWinAmountsStep(),
            WalletDepositStep(walletAdapter),
            SaveSettleSpinStep(spinRepository),
            PublishSpinSettledEventStep(eventPublisher, gameService)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: SettleSpinContext): Result<Unit> {
        // Skip if no win amount
        if (context.winAmount <= BigInteger.ZERO) {
            return Result.success(Unit)
        }
        return orchestrator.execute(context)
    }
}
