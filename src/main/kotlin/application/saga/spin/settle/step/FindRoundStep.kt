package application.saga.spin.settle.step

import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.RoundNotFoundError
import domain.session.repository.RoundRepository

/**
 * Step 1: Find the round by external ID.
 */
class FindRoundStep(
    private val roundRepository: RoundRepository
) : ValidationStep<SettleSpinContext>("find_round", "Find Round") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = roundRepository.findByExtId(context.session.id, context.extRoundId)
            ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        return Result.success(Unit)
    }
}
