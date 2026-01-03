package application.saga.spin.place.step

import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import domain.common.value.SpinType
import domain.session.model.Spin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigInteger
import java.util.UUID

/**
 * Step 5: Save spin record (AFTER successful wallet operation).
 * Uses direct Exposed DSL for database access.
 */
class SavePlaceSpinStep : SagaStep<PlaceSpinContext> {

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

        val savedSpin = newSuspendedTransaction {
            val id = SpinTable.insertAndGetId {
                it[roundId] = spin.roundId
                it[type] = spin.type
                it[amount] = spin.amount.toLong()
                it[realAmount] = spin.realAmount.toLong()
                it[bonusAmount] = spin.bonusAmount.toLong()
                it[extId] = spin.extId
                it[referenceId] = spin.referenceId
                it[freeSpinId] = spin.freeSpinId
            }
            spin.copy(id = id.value)
        }
        context.spin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        val spin = context.spin ?: return Result.success(Unit)

        // Create a rollback spin record for audit trail
        // We don't delete - we mark as rolled back
        newSuspendedTransaction {
            SpinTable.insertAndGetId {
                it[roundId] = spin.roundId
                it[type] = SpinType.ROLLBACK
                it[amount] = 0L
                it[realAmount] = 0L
                it[bonusAmount] = 0L
                it[extId] = "${spin.extId}_rollback_${context.sagaId}"
                it[referenceId] = spin.id
                it[freeSpinId] = spin.freeSpinId
            }
        }
        return Result.success(Unit)
    }
}
