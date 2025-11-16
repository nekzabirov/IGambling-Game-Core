package app.usecase

import domain.game.table.GameTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateGameUsecase {
    suspend operator fun invoke(
        identity: String,
        active: Boolean,
        bonusBet: Boolean,
        bonusWagering: Boolean
    ): Result<Unit> = newSuspendedTransaction {
        GameTable.update({ GameTable.identity eq identity }) {
            it[GameTable.active] = active
            it[GameTable.bonusBetEnable] = bonusBet
            it[GameTable.bonusWageringEnable] = bonusWagering
        }

        Result.success(Unit)
    }
}
