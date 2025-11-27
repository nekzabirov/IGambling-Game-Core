package app.usecase

import domain.aggregator.mapper.toAggregatorModel
import domain.aggregator.model.Aggregator
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import domain.provider.mapper.toProvider
import domain.provider.table.ProviderTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AssignProviderToAggregatorUsecase {
    suspend operator fun invoke(providerId: UUID, aggregatorId: UUID): Result<Unit> =
        newSuspendedTransaction {
            val provider = ProviderTable
                .selectAll()
                .where { ProviderTable.id eq providerId }
                .singleOrNull()?.toProvider()
                ?: return@newSuspendedTransaction Result.failure(IllegalArgumentException("Provider not found"))

            val aggregator = AggregatorInfoTable
                .selectAll()
                .where { AggregatorInfoTable.id eq aggregatorId }
                .singleOrNull()?.toAggregatorModel()
                ?: return@newSuspendedTransaction Result.failure(IllegalArgumentException("Aggregator not found"))

            ProviderTable.update(where = { ProviderTable.id eq providerId }) {
                it[ProviderTable.aggregatorId] = aggregator.id
            }

            Result.success(Unit)
        }
}