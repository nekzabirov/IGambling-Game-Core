package application.saga.spin.place.step

import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import domain.session.repository.RoundRepository

/**
 * Step 2: Find or create round.
 */
class FindOrCreateRoundStep(
    private val roundRepository: RoundRepository
) : SagaStep<PlaceSpinContext> {

    override val stepId = "find_or_create_round"
    override val stepName = "Find or Create Round"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val game = context.game ?: return Result.failure(
            IllegalStateError("find_round", "game not set in context")
        )

        val existingRound = roundRepository.findByExtId(context.session.id, context.extRoundId)

        val round = if (existingRound != null) {
            existingRound
        } else {
            context.put(PlaceSpinContext.KEY_ROUND_CREATED, true)
            roundRepository.findOrCreate(context.session.id, game.id, context.extRoundId)
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
