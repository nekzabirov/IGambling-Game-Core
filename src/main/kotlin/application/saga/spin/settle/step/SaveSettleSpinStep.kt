package application.saga.spin.settle.step

import application.saga.SagaStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.IllegalStateError
import domain.common.value.SpinType
import domain.session.model.Spin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigInteger
import java.util.UUID

/**
 * Step 5: Save settle spin record (AFTER successful wallet operation).
 * Uses direct Exposed DSL for database access.
 */
class SaveSettleSpinStep : SagaStep<SettleSpinContext> {

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

        val savedSpin = newSuspendedTransaction {
            val id = SpinTable.insertAndGetId {
                it[roundId] = settleSpin.roundId
                it[type] = settleSpin.type
                it[amount] = settleSpin.amount.toLong()
                it[realAmount] = settleSpin.realAmount.toLong()
                it[bonusAmount] = settleSpin.bonusAmount.toLong()
                it[extId] = settleSpin.extId
                it[referenceId] = settleSpin.referenceId
                it[freeSpinId] = settleSpin.freeSpinId
            }
            settleSpin.copy(id = id.value)
        }
        context.settleSpin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: SettleSpinContext): Result<Unit> {
        val settleSpin = context.settleSpin ?: return Result.success(Unit)

        // Create a rollback spin record for audit trail
        newSuspendedTransaction {
            SpinTable.insertAndGetId {
                it[roundId] = settleSpin.roundId
                it[type] = SpinType.ROLLBACK
                it[amount] = 0L
                it[realAmount] = 0L
                it[bonusAmount] = 0L
                it[extId] = "${settleSpin.extId}_rollback_${context.sagaId}"
                it[referenceId] = settleSpin.id
                it[freeSpinId] = settleSpin.freeSpinId
            }
        }
        return Result.success(Unit)
    }
}
