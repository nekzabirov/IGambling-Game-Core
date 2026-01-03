package application.saga.spin.rollback.step

import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.RoundNotFoundError
import infrastructure.persistence.exposed.mapper.toRound
import infrastructure.persistence.exposed.table.RoundTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Step 1: Find the round by external ID.
 * Uses direct Exposed DSL for database access.
 */
class FindRoundStep : ValidationStep<RollbackSpinContext>("find_round", "Find Round") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = newSuspendedTransaction {
            RoundTable.selectAll()
                .where { (RoundTable.sessionId eq context.session.id) and (RoundTable.extId eq context.extRoundId) }
                .singleOrNull()
                ?.toRound()
        } ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        return Result.success(Unit)
    }
}
