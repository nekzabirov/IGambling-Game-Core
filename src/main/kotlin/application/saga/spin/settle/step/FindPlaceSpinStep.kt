package application.saga.spin.settle.step

import application.port.outbound.SpinRepository
import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.IllegalStateError
import domain.common.error.RoundFinishedError
import domain.common.value.SpinType

/**
 * Step 2: Find the original place spin.
 */
class FindPlaceSpinStep(
    private val spinRepository: SpinRepository
) : ValidationStep<SettleSpinContext>("find_place_spin", "Find Place Spin") {

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("find_place_spin", "round not set")
        )

        val placeSpin = spinRepository.findByRoundAndType(round.id, SpinType.PLACE)
            ?: return Result.failure(RoundFinishedError(context.extRoundId))

        context.placeSpin = placeSpin
        return Result.success(Unit)
    }
}
