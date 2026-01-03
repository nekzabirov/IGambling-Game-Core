package application.saga.spin.settle.step

import application.port.outbound.RoundRepository
import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.RoundNotFoundError

/**
 * Optimized step: Find round AND place spin in a single query.
 * Replaces separate FindRoundStep + FindPlaceSpinStep for better performance.
 */
class FindRoundWithSpinStep(
    private val roundRepository: RoundRepository
) : ValidationStep<SettleSpinContext>("find_round_with_spin", "Find Round With Spin") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val (round, placeSpin) = roundRepository.findWithPlaceSpin(
            context.session.id,
            context.extRoundId
        ) ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.round = round
        context.placeSpin = placeSpin
        return Result.success(Unit)
    }
}
