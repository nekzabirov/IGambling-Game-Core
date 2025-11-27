package app.usecase

import domain.provider.table.ProviderTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateProviderUsecase {
    suspend operator fun invoke(identity: String, order: Int, active: Boolean): Result<Unit> = newSuspendedTransaction {
        ProviderTable.update({ ProviderTable.identity eq identity }) {
            it[ProviderTable.order] = order
            it[ProviderTable.active] = active
        }

        Result.success(Unit)
    }
}