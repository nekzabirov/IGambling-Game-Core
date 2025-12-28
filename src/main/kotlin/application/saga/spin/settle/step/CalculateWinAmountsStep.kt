package application.saga.spin.settle.step

import application.saga.ValidationStep
import application.saga.spin.settle.SettleSpinContext
import domain.common.error.IllegalStateError
import java.math.BigInteger

/**
 * Step 3: Calculate win amounts (real vs bonus).
 */
class CalculateWinAmountsStep : ValidationStep<SettleSpinContext>(
    "calculate_win_amounts",
    "Calculate Win Amounts"
) {
    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        val placeSpin = context.placeSpin ?: return Result.failure(
            IllegalStateError("calculate_win_amounts", "placeSpin not set")
        )

        if (context.isFreeSpin) {
            // FreeSpin mode: all goes to real
            context.realAmount = context.winAmount
            context.bonusAmount = BigInteger.ZERO
        } else {
            // Determine if bonus was used in the original bet
            val isBonusUsed = placeSpin.bonusAmount > BigInteger.ZERO

            // Winnings go to bonus if original bet used bonus
            context.realAmount = if (isBonusUsed) BigInteger.ZERO else context.winAmount
            context.bonusAmount = if (isBonusUsed) context.winAmount else BigInteger.ZERO
        }

        return Result.success(Unit)
    }
}
