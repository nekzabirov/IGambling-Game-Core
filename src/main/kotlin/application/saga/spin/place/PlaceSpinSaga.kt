package application.saga.spin.place

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.PlayerAdapter
import application.port.outbound.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.place.step.*
import application.service.AggregatorService
import application.service.GameService
import domain.session.repository.RoundRepository
import domain.session.repository.SpinRepository

/**
 * Saga definition for placing a spin (bet) operation.
 * Ensures atomic execution with automatic compensation on failure.
 *
 * **CRITICAL FIX**: This saga executes wallet withdrawal BEFORE saving spin.
 * Previous implementation had a bug where spin was saved first, causing
 * orphan records if wallet withdrawal failed.
 *
 * Step order:
 * 1. ValidateGame - validate aggregator and game exist
 * 2. FindOrCreateRound - create/get round
 * 3. ValidateBalance - check player has sufficient funds
 * 4. WalletWithdraw - withdraw from wallet (BEFORE saving)
 * 5. SaveSpin - save spin record (AFTER wallet success)
 * 6. PublishEvent - publish domain event
 */
class PlaceSpinSaga(
    private val aggregatorService: AggregatorService,
    private val gameService: GameService,
    private val walletAdapter: WalletAdapter,
    private val playerAdapter: PlayerAdapter,
    private val roundRepository: RoundRepository,
    private val spinRepository: SpinRepository,
    private val eventPublisher: EventPublisherAdapter
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "PlaceSpinSaga",
        steps = listOf(
            ValidateGameStep(aggregatorService, gameService),
            FindOrCreateRoundStep(roundRepository),
            ValidateBalanceStep(walletAdapter, playerAdapter),
            WalletWithdrawStep(walletAdapter),
            SavePlaceSpinStep(spinRepository),
            PublishSpinPlacedEventStep(eventPublisher)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        return orchestrator.execute(context)
    }
}
