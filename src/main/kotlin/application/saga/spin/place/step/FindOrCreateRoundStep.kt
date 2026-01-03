package application.saga.spin.place.step

import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import infrastructure.persistence.exposed.mapper.toRound
import infrastructure.persistence.exposed.table.RoundTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsertReturning

/**
 * Step 2: Find or create round.
 * Uses direct Exposed DSL for database access.
 */
class FindOrCreateRoundStep : SagaStep<PlaceSpinContext> {

    override val stepId = "find_or_create_round"
    override val stepName = "Find or Create Round"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val game = context.game ?: return Result.failure(
            IllegalStateError("find_round", "game not set in context")
        )

        val round = newSuspendedTransaction {
            // Check if round exists
            val existingRound = RoundTable.selectAll()
                .where { (RoundTable.sessionId eq context.session.id) and (RoundTable.extId eq context.extRoundId) }
                .singleOrNull()
                ?.toRound()

            if (existingRound != null) {
                existingRound
            } else {
                context.put(PlaceSpinContext.KEY_ROUND_CREATED, true)
                // Atomic upsert using unique constraint on (sessionId, extId)
                val row = RoundTable.upsertReturning(
                    keys = arrayOf(RoundTable.sessionId, RoundTable.extId),
                    onUpdateExclude = listOf(RoundTable.id, RoundTable.gameId, RoundTable.finished)
                ) {
                    it[sessionId] = context.session.id
                    it[gameId] = game.id
                    it[extId] = context.extRoundId
                    it[finished] = false
                }.single()
                row.toRound()
            }
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
