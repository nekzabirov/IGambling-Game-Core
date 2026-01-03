package application.saga.spin.settle.step

import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.IllegalStateError
import domain.common.error.RoundFinishedError
import domain.common.value.SpinType
import infrastructure.persistence.exposed.mapper.toSpin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Step 2: Find the original place spin.
 * Uses direct Exposed DSL for database access.
 */
class FindPlaceSpinStep : ValidationStep<SettleSpinContext>("find_place_spin", "Find Place Spin") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("find_place_spin", "round not set")
        )

        val placeSpin = newSuspendedTransaction {
            SpinTable.selectAll()
                .where { (SpinTable.roundId eq round.id) and (SpinTable.type eq SpinType.PLACE) }
                .singleOrNull()
                ?.toSpin()
        } ?: return Result.failure(RoundFinishedError(context.extRoundId))

        context.placeSpin = placeSpin
        return Result.success(Unit)
    }
}
