package application.saga.spin.place.step

import application.port.outbound.WalletAdapter
import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext

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

        val newBalance = walletAdapter.withdraw(
            playerId = context.session.playerId,
            transactionId = txId,
            currency = context.session.currency,
            realAmount = context.betRealAmount,
            bonusAmount = context.betBonusAmount
        ).getOrElse {
            return Result.failure(it)
        }

        context.resultBalance = newBalance
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
