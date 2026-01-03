package application.saga.spin.rollback.step

import application.port.outbound.SpinRepository
import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.IllegalStateError
import domain.common.error.RoundNotFoundError
import domain.common.value.SpinType

/**
 * Step 2: Find the original spin to rollback.
 */
class FindOriginalSpinStep(
    private val spinRepository: SpinRepository
) : ValidationStep<RollbackSpinContext>("find_original_spin", "Find Original Spin") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("find_original_spin", "round not set")
        )

        val spin = spinRepository.findByRoundAndType(round.id, SpinType.PLACE)
            ?: return Result.failure(RoundNotFoundError(context.extRoundId))

        context.originalSpin = spin
        context.refundRealAmount = spin.realAmount
        context.refundBonusAmount = spin.bonusAmount

        return Result.success(Unit)
    }
}
