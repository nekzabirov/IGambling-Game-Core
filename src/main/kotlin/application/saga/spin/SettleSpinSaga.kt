package application.saga.spin

import domain.common.event.SpinSettledEvent
import application.port.outbound.EventPublisherAdapter
import application.port.outbound.WalletAdapter
import application.saga.BaseSagaContext
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.SagaStep
import application.saga.ValidationStep
import application.service.GameService
import domain.common.error.*
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import domain.session.repository.RoundRepository
import domain.session.repository.SpinRepository
import domain.common.value.SpinType
import java.math.BigInteger
import java.util.UUID

/**
 * Context for SettleSpin saga.
 */
class SettleSpinContext(
    val session: Session,
    val extRoundId: String,
    val transactionId: String,
    val freeSpinId: String?,
    val winAmount: BigInteger,
    correlationId: String = transactionId
) : BaseSagaContext(correlationId = correlationId) {

    var round: Round? = null
    var placeSpin: Spin? = null
    var settleSpin: Spin? = null
    var realAmount: BigInteger = BigInteger.ZERO
    var bonusAmount: BigInteger = BigInteger.ZERO
    var gameIdentity: String = ""

    val isFreeSpin: Boolean get() = freeSpinId != null

    companion object {
        const val KEY_WALLET_TX_COMPLETED = "wallet_tx_completed"
    }
}

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

// ============ Individual Saga Steps ============

/**
 * Step 1: Find the round by external ID.
 */
class FindRoundStep(
    private val roundRepository: RoundRepository
) : ValidationStep<SettleSpinContext>("find_round", "Find Round") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = roundRepository.findByExtId(context.session.id, context.extRoundId)
            ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        return Result.success(Unit)
    }
}

/**
 * Step 2: Find the original place spin.
 */
class FindPlaceSpinStep(
    private val spinRepository: SpinRepository
) : ValidationStep<SettleSpinContext>("find_place_spin", "Find Place Spin") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("find_place_spin", "round not set")
        )

        val placeSpin = spinRepository.findPlaceSpinByRoundId(round.id)
            ?: return Result.failure(RoundFinishedError(context.extRoundId))

        context.placeSpin = placeSpin
        return Result.success(Unit)
    }
}

/**
 * Step 3: Calculate win amounts (real vs bonus).
 */
class CalculateWinAmountsStep : ValidationStep<SettleSpinContext>(
    "calculate_win_amounts",
    "Calculate Win Amounts"
) {
    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val placeSpin = context.placeSpin ?: return Result.failure(
            IllegalStateError("calculate_win_amounts", "placeSpin not set")
        )

        if (context.isFreeSpin) {
            // FreeSpin mode: all goes to real
            context.realAmount = context.winAmount
            context.bonusAmount = BigInteger.ZERO
        } else {
            // Determine if bonus was used in the original bet
            val isBonusUsed = placeSpin.bonusAmount > BigInteger.ZERO

            // Winnings go to bonus if original bet used bonus
            context.realAmount = if (isBonusUsed) BigInteger.ZERO else context.winAmount
            context.bonusAmount = if (isBonusUsed) context.winAmount else BigInteger.ZERO
        }

        return Result.success(Unit)
    }
}

/**
 * Step 4: Deposit winnings to wallet (BEFORE saving settle spin).
 */
class WalletDepositStep(
    private val walletAdapter: WalletAdapter
) : SagaStep<SettleSpinContext> {

    override val stepId = "wallet_deposit"
    override val stepName = "Wallet Deposit"
    override val requiresCompensation = true

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        if (context.isFreeSpin) {
            // FreeSpin mode: skip wallet operation
            return Result.success(Unit)
        }

        // Use saga ID as transaction ID for idempotency
        val txId = context.sagaId.toString()

        walletAdapter.deposit(
            playerId = context.session.playerId,
            transactionId = txId,
            currency = context.session.currency,
            realAmount = context.realAmount,
            bonusAmount = context.bonusAmount
        ).getOrElse {
            return Result.failure(it)
        }

        context.put(SettleSpinContext.KEY_WALLET_TX_COMPLETED, true)
        return Result.success(Unit)
    }

    override suspend fun compensate(context: SettleSpinContext): Result<Unit> {
        if (context.isFreeSpin) return Result.success(Unit)

        val walletTxCompleted = context.get<Boolean>(SettleSpinContext.KEY_WALLET_TX_COMPLETED) ?: false
        if (!walletTxCompleted) return Result.success(Unit)

        // Rollback the wallet deposit (withdraw the winnings back)
        return walletAdapter.rollback(
            context.session.playerId,
            context.sagaId.toString()
        )
    }
}

/**
 * Step 5: Save settle spin record (AFTER successful wallet operation).
 */
class SaveSettleSpinStep(
    private val spinRepository: SpinRepository
) : SagaStep<SettleSpinContext> {

    override val stepId = "save_settle_spin"
    override val stepName = "Save Settle Spin Record"
    override val requiresCompensation = true

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("save_settle_spin", "round not set")
        )
        val placeSpin = context.placeSpin ?: return Result.failure(
            IllegalStateError("save_settle_spin", "placeSpin not set")
        )

        val settleSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.SETTLE,
            amount = context.winAmount,
            realAmount = context.realAmount,
            bonusAmount = context.bonusAmount,
            extId = context.transactionId,
            referenceId = placeSpin.id,
            freeSpinId = context.freeSpinId
        )

        val savedSpin = spinRepository.save(settleSpin)
        context.settleSpin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: SettleSpinContext): Result<Unit> {
        val settleSpin = context.settleSpin ?: return Result.success(Unit)

        // Create a rollback spin record for audit trail
        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = settleSpin.roundId,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            extId = "${settleSpin.extId}_rollback_${context.sagaId}",
            referenceId = settleSpin.id,
            freeSpinId = settleSpin.freeSpinId
        )

        spinRepository.save(rollbackSpin)
        return Result.success(Unit)
    }
}

/**
 * Step 6: Publish spin settled event.
 */
class PublishSpinSettledEventStep(
    private val eventPublisher: EventPublisherAdapter,
    private val gameService: GameService
) : ValidationStep<SettleSpinContext>("publish_settled_event", "Publish Spin Settled Event") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        // Get game identity for the event
        val game = gameService.findById(context.session.gameId).getOrElse {
            // Non-critical: log warning but don't fail
            context.gameIdentity = "unknown"
            return Result.success(Unit)
        }

        context.gameIdentity = game.identity

        eventPublisher.publish(
            SpinSettledEvent(
                gameIdentity = game.identity,
                amount = context.winAmount,
                currency = context.session.currency,
                playerId = context.session.playerId,
                freeSpinId = context.freeSpinId
            )
        )

        return Result.success(Unit)
    }
}
