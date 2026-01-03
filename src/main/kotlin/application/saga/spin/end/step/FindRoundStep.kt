package application.saga.spin.end.step

import application.port.outbound.RoundRepository
import application.saga.ValidationStep
import application.saga.spin.end.EndSpinContext
import domain.common.error.RoundNotFoundError

/**
 * Step 1: Find the round by external ID.
 */
class FindRoundStep(
    private val roundRepository: RoundRepository
) : ValidationStep<EndSpinContext>("find_round", "Find Round") {

    override suspend fun execute(context: EndSpinContext): Result<Unit> {
        val round = roundRepository.findBySessionAndExtId(
            context.session.id,
            context.extRoundId
        ) ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        return Result.success(Unit)
    }
}
