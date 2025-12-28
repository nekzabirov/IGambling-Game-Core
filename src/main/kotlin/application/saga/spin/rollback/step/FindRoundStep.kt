package application.saga.spin.rollback.step

import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.RoundNotFoundError
import domain.session.repository.RoundRepository

/**
 * Step 1: Find the round by external ID.
 */
class FindRoundStep(
    private val roundRepository: RoundRepository
) : ValidationStep<RollbackSpinContext>("find_round", "Find Round") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = roundRepository.findByExtId(context.session.id, context.extRoundId)
            ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        return Result.success(Unit)
    }
}
