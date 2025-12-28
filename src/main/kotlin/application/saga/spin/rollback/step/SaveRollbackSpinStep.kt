package application.saga.spin.rollback.step

import application.saga.SagaStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.IllegalStateError
import domain.common.value.SpinType
import domain.session.model.Spin
import domain.session.repository.SpinRepository
import java.math.BigInteger
import java.util.UUID

/**
 * Step 4: Save rollback spin record.
 */
class SaveRollbackSpinStep(
    private val spinRepository: SpinRepository
) : SagaStep<RollbackSpinContext> {

    override val stepId = "save_rollback_spin"
    override val stepName = "Save Rollback Spin Record"
    override val requiresCompensation = false // Rollback record is for audit, no need to undo

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("save_rollback_spin", "round not set")
        )
        val originalSpin = context.originalSpin ?: return Result.failure(
            IllegalStateError("save_rollback_spin", "originalSpin not set")
        )

        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            extId = context.transactionId,
            referenceId = originalSpin.id,
            freeSpinId = originalSpin.freeSpinId
        )

        val savedSpin = spinRepository.save(rollbackSpin)
        context.rollbackSpin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: RollbackSpinContext): Result<Unit> {
        // No compensation needed - rollback records are audit trail
        return Result.success(Unit)
    }
}
