package app.usecase

import domain.aggregator.model.Aggregator
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddAggregatorUsecase {
    suspend operator fun invoke(
        identity: String,
        type: Aggregator,
        config: Map<String, String>
    ): Result<AggregatorInfo> = newSuspendedTransaction {
        val id = AggregatorInfoTable.insertAndGetId {
            it[AggregatorInfoTable.identity] = identity
            it[aggregator] = Aggregator.ONEGAMEHUB
            it[AggregatorInfoTable.config] = config
        }.value

        Result.success(
            AggregatorInfo(
                id = id,
                identity = identity,
                config = config,
                aggregator = type,
                active = true,
            )
        )
    }
}