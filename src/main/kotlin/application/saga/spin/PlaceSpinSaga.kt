package application.saga.spin

import domain.common.event.SpinPlacedEvent
import application.port.outbound.EventPublisherAdapter
import application.port.outbound.PlayerAdapter
import application.port.outbound.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.SagaStep
import application.saga.ValidationStep
import application.service.GameService
import application.service.AggregatorService
import domain.common.error.*
import domain.session.model.Spin
import domain.session.repository.RoundRepository
import domain.session.repository.SpinRepository
import domain.common.value.SpinType
import java.math.BigInteger
import java.util.UUID

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
            WalletWithdrawStep(walletAdapter),  // CRITICAL: Wallet BEFORE saving spin
            SaveSpinStep(spinRepository),        // CRITICAL: Save AFTER wallet success
            PublishSpinPlacedEventStep(eventPublisher)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        return orchestrator.execute(context)
    }
}

// ============ Individual Saga Steps ============

/**
 * Step 1: Validate aggregator and game.
 */
class ValidateGameStep(
    private val aggregatorService: AggregatorService,
    private val gameService: GameService
) : ValidationStep<PlaceSpinContext>("validate_game", "Validate Game") {

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val aggregator = aggregatorService.findById(context.session.aggregatorId).getOrElse {
            return Result.failure(it)
        }

        val game = gameService.findBySymbol(
            symbol = context.gameSymbol,
            aggregator = aggregator.aggregator
        ).getOrElse {
            return Result.failure(it)
        }

        if (!game.isPlayable()) {
            return Result.failure(GameUnavailableError(context.gameSymbol))
        }

        context.game = game
        return Result.success(Unit)
    }
}

/**
 * Step 2: Find or create round.
 */
class FindOrCreateRoundStep(
    private val roundRepository: RoundRepository
) : SagaStep<PlaceSpinContext> {

    override val stepId = "find_or_create_round"
    override val stepName = "Find or Create Round"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val game = context.game ?: return Result.failure(
            IllegalStateError("find_round", "game not set in context")
        )

        val existingRound = roundRepository.findByExtId(context.session.id, context.extRoundId)

        val round = if (existingRound != null) {
            existingRound
        } else {
            context.put(PlaceSpinContext.KEY_ROUND_CREATED, true)
            roundRepository.findOrCreate(context.session.id, game.id, context.extRoundId)
        }

        context.round = round
        return Result.success(Unit)
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        // Only compensate if we created a new round and no spin was saved
        // For audit purposes, we keep the round but could mark it as cancelled
        val roundCreated = context.get<Boolean>(PlaceSpinContext.KEY_ROUND_CREATED) ?: false
        if (roundCreated && context.spin == null) {
            // Round was created but no spin saved - could mark as cancelled
            // For now, we leave it as-is since empty rounds are harmless
        }
        return Result.success(Unit)
    }
}

/**
 * Step 3: Validate balance and bet limits (skip for freespins).
 */
class ValidateBalanceStep(
    private val walletAdapter: WalletAdapter,
    private val playerAdapter: PlayerAdapter
) : ValidationStep<PlaceSpinContext>("validate_balance", "Validate Balance") {

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        if (context.isFreeSpin) {
            // FreeSpin mode: skip balance validation
            context.betRealAmount = BigInteger.ZERO
            context.betBonusAmount = BigInteger.ZERO
            return Result.success(Unit)
        }

        val game = context.game ?: return Result.failure(
            IllegalStateError("validate_balance", "game not set")
        )

        // Fetch balance and bet limit
        val balance = walletAdapter.findBalance(context.session.playerId).getOrElse {
            return Result.failure(it)
        }

        val betLimit = playerAdapter.findCurrentBetLimit(context.session.playerId).getOrElse {
            return Result.failure(it)
        }

        // Adjust balance if bonus bet is disabled
        val adjustedBalance = if (!game.bonusBetEnable) {
            balance.copy(bonus = BigInteger.ZERO)
        } else {
            balance
        }

        // Validate bet limit
        if (betLimit != null && betLimit < context.amount) {
            return Result.failure(
                BetLimitExceededError(context.session.playerId, context.amount, betLimit)
            )
        }

        // Validate sufficient balance
        if (context.amount > adjustedBalance.totalAmount) {
            return Result.failure(
                InsufficientBalanceError(context.session.playerId, context.amount, adjustedBalance.totalAmount)
            )
        }

        // Calculate real and bonus amounts
        context.betRealAmount = minOf(context.amount, adjustedBalance.real)
        context.betBonusAmount = context.amount - context.betRealAmount
        context.balance = adjustedBalance

        return Result.success(Unit)
    }
}

/**
 * Step 4: Withdraw from wallet (BEFORE saving spin).
 * This is the key change - we now do wallet operation first!
 */
class WalletWithdrawStep(
    private val walletAdapter: WalletAdapter
) : SagaStep<PlaceSpinContext> {

    override val stepId = "wallet_withdraw"
    override val stepName = "Wallet Withdraw"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        if (context.isFreeSpin) {
            // FreeSpin mode: skip wallet operation
            return Result.success(Unit)
        }

        // Use saga ID as transaction ID for idempotency
        val txId = context.sagaId.toString()

        walletAdapter.withdraw(
            playerId = context.session.playerId,
            transactionId = txId,
            currency = context.session.currency,
            realAmount = context.betRealAmount,
            bonusAmount = context.betBonusAmount
        ).getOrElse {
            return Result.failure(it)
        }

        context.put(PlaceSpinContext.KEY_WALLET_TX_COMPLETED, true)
        return Result.success(Unit)
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        if (context.isFreeSpin) return Result.success(Unit)

        val walletTxCompleted = context.get<Boolean>(PlaceSpinContext.KEY_WALLET_TX_COMPLETED) ?: false
        if (!walletTxCompleted) return Result.success(Unit)

        // Rollback the wallet transaction
        return walletAdapter.rollback(
            context.session.playerId,
            context.sagaId.toString()
        )
    }
}

/**
 * Step 5: Save spin record (AFTER successful wallet operation).
 */
class SaveSpinStep(
    private val spinRepository: SpinRepository
) : SagaStep<PlaceSpinContext> {

    override val stepId = "save_spin"
    override val stepName = "Save Spin Record"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("save_spin", "round not set")
        )

        val spin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.PLACE,
            amount = context.amount,
            realAmount = context.betRealAmount,
            bonusAmount = context.betBonusAmount,
            extId = context.transactionId,
            freeSpinId = context.freeSpinId
        )

        val savedSpin = spinRepository.save(spin)
        context.spin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        val spin = context.spin ?: return Result.success(Unit)

        // Create a rollback spin record for audit trail
        // We don't delete - we mark as rolled back
        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = spin.roundId,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            extId = "${spin.extId}_rollback_${context.sagaId}",
            referenceId = spin.id,
            freeSpinId = spin.freeSpinId
        )

        spinRepository.save(rollbackSpin)
        return Result.success(Unit)
    }
}

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
