package application.saga.spin.end.step

import application.saga.SagaStep
import application.saga.spin.end.EndSpinContext
import domain.common.error.IllegalStateError
import domain.session.repository.RoundRepository

/**
 * Step 2: Close/finish the round.
 */
class CloseRoundStep(
    private val roundRepository: RoundRepository
) : SagaStep<EndSpinContext> {

    override val stepId = "close_round"
    override val stepName = "Close Round"
    override val requiresCompensation = false // Closing a round doesn't need rollback

    override suspend fun execute(context: EndSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("close_round", "round not set")
        )

        roundRepository.finish(round.id)
        return Result.success(Unit)
    }

    override suspend fun compensate(context: EndSpinContext): Result<Unit> {
        // No compensation needed - reopening a round is not a valid operation
        return Result.success(Unit)
    }
}
