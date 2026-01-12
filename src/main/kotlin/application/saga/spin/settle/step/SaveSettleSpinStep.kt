package application.saga.spin.settle.step

import application.port.outbound.SpinRepository
import application.saga.SagaStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.IllegalStateError
import domain.common.value.SpinType
import domain.session.model.Spin
import java.util.UUID

/**
 * Step 5: Save settle spin record (AFTER successful wallet operation).
 */
class SaveSettleSpinStep(
    private val spinRepository: SpinRepository
) : SagaStep<SettleSpinContext> {

    override val stepId = "save_settle_spin"
    override val stepName = "Save Settle Spin Record"
    override val requiresCompensation = true

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("save_settle_spin", "round not set")
        )
        val placeSpin = context.placeSpin ?: return Result.failure(
            IllegalStateError("save_settle_spin", "placeSpin not set")
        )

        val settleSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.SETTLE,
            amount = context.winAmount,
            realAmount = context.realAmount,
            bonusAmount = context.bonusAmount,
            extId = context.transactionId,
            referenceId = placeSpin.id,
            freeSpinId = context.freeSpinId
        )

        context.settleSpin = spinRepository.save(settleSpin)
        return Result.success(Unit)
    }

    override suspend fun compensate(context: SettleSpinContext): Result<Unit> {
        val settleSpin = context.settleSpin ?: return Result.success(Unit)

        // Create a rollback spin record for audit trail
        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = settleSpin.roundId,
            type = SpinType.ROLLBACK,
            amount = 0L,
            realAmount = 0L,
            bonusAmount = 0L,
            extId = "${settleSpin.extId}_rollback_${context.sagaId}",
            referenceId = settleSpin.id,
            freeSpinId = settleSpin.freeSpinId
        )
        spinRepository.save(rollbackSpin)
        return Result.success(Unit)
    }
}
