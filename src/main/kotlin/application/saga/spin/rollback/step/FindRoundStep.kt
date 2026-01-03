package application.saga.spin.rollback.step

import application.port.outbound.RoundRepository
import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.RoundNotFoundError

/**
 * Step 1: Find the round by external ID.
 */
class FindRoundStep(
    private val roundRepository: RoundRepository
) : ValidationStep<RollbackSpinContext>("find_round", "Find Round") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = roundRepository.findBySessionAndExtId(
            context.session.id,
            context.extRoundId
        ) ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        return Result.success(Unit)
    }
}
