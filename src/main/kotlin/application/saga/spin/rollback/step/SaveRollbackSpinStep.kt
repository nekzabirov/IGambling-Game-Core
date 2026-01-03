package application.saga.spin.rollback.step

import application.saga.SagaStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.IllegalStateError
import domain.common.value.SpinType
import domain.session.model.Spin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigInteger
import java.util.UUID

/**
 * Step 4: Save rollback spin record.
 * Uses direct Exposed DSL for database access.
 */
class SaveRollbackSpinStep : SagaStep<RollbackSpinContext> {

    override val stepId = "save_rollback_spin"
    override val stepName = "Save Rollback Spin Record"
    override val requiresCompensation = false // Rollback record is for audit, no need to undo

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("save_rollback_spin", "round not set")
        )
        val originalSpin = context.originalSpin ?: return Result.failure(
            IllegalStateError("save_rollback_spin", "originalSpin not set")
        )

        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            extId = context.transactionId,
            referenceId = originalSpin.id,
            freeSpinId = originalSpin.freeSpinId
        )

        val savedSpin = newSuspendedTransaction {
            val id = SpinTable.insertAndGetId {
                it[roundId] = rollbackSpin.roundId
                it[type] = rollbackSpin.type
                it[amount] = rollbackSpin.amount.toLong()
                it[realAmount] = rollbackSpin.realAmount.toLong()
                it[bonusAmount] = rollbackSpin.bonusAmount.toLong()
                it[extId] = rollbackSpin.extId
                it[referenceId] = rollbackSpin.referenceId
                it[freeSpinId] = rollbackSpin.freeSpinId
            }
            rollbackSpin.copy(id = id.value)
        }
        context.rollbackSpin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: RollbackSpinContext): Result<Unit> {
        // No compensation needed - rollback records are audit trail
        return Result.success(Unit)
    }
}
