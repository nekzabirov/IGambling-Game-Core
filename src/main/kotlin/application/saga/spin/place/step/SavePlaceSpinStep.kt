package application.saga.spin.place.step

import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import domain.common.value.SpinType
import domain.session.model.Spin
import domain.session.repository.SpinRepository
import java.math.BigInteger
import java.util.UUID

/**
 * Step 5: Save spin record (AFTER successful wallet operation).
 */
class SavePlaceSpinStep(
    private val spinRepository: SpinRepository
) : SagaStep<PlaceSpinContext> {

    override val stepId = "save_spin"
    override val stepName = "Save Spin Record"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val round = context.round ?: return Result.failure(
            IllegalStateError("save_spin", "round not set")
        )

        val spin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.PLACE,
            amount = context.amount,
            realAmount = context.betRealAmount,
            bonusAmount = context.betBonusAmount,
            extId = context.transactionId,
            freeSpinId = context.freeSpinId
        )

        val savedSpin = spinRepository.save(spin)
        context.spin = savedSpin

        return Result.success(Unit)
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        val spin = context.spin ?: return Result.success(Unit)

        // Create a rollback spin record for audit trail
        // We don't delete - we mark as rolled back
        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = spin.roundId,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            extId = "${spin.extId}_rollback_${context.sagaId}",
            referenceId = spin.id,
            freeSpinId = spin.freeSpinId
        )

        spinRepository.save(rollbackSpin)
        return Result.success(Unit)
    }
}
