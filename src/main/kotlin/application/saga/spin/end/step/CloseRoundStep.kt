package application.saga.spin.end.step

import application.saga.SagaStep
import application.saga.spin.end.EndSpinContext
import domain.common.error.IllegalStateError
import infrastructure.persistence.exposed.table.RoundTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Step 2: Close/finish the round.
 * Uses direct Exposed DSL for database access.
 */
class CloseRoundStep : SagaStep<EndSpinContext> {

    override val stepId = "close_round"
    override val stepName = "Close Round"
    override val requiresCompensation = false // Closing a round doesn't need rollback

    override suspend fun execute(context: EndSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("close_round", "round not set")
        )

        newSuspendedTransaction {
            RoundTable.update({ RoundTable.id eq round.id }) {
                it[finished] = true
                it[finishedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun compensate(context: EndSpinContext): Result<Unit> {
        // No compensation needed - reopening a round is not a valid operation
        return Result.success(Unit)
    }
}
