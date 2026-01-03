package application.saga.spin.rollback.step

import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.IllegalStateError
import domain.common.error.RoundNotFoundError
import domain.common.value.SpinType
import infrastructure.persistence.exposed.mapper.toSpin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Step 2: Find the original spin to rollback.
 * Uses direct Exposed DSL for database access.
 */
class FindOriginalSpinStep : ValidationStep<RollbackSpinContext>("find_original_spin", "Find Original Spin") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("find_original_spin", "round not set")
        )

        val spin = newSuspendedTransaction {
            SpinTable.selectAll()
                .where { (SpinTable.roundId eq round.id) and (SpinTable.type eq SpinType.PLACE) }
                .singleOrNull()
                ?.toSpin()
        } ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.originalSpin = spin
        context.refundRealAmount = spin.realAmount
        context.refundBonusAmount = spin.bonusAmount

        return Result.success(Unit)
    }
}
