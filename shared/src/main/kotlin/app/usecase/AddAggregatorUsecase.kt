package app.usecase

import domain.value.Aggregator
import domain.model.AggregatorInfo
import domain.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddAggregatorUsecase {
    suspend operator fun invoke(
        identity: String,
        type: domain.value.Aggregator,
        config: Map<String, String>
    ): Result<domain.model.AggregatorInfo> = newSuspendedTransaction {
        val id = AggregatorInfoTable.insertAndGetId {
            it[AggregatorInfoTable.identity] = identity
            it[aggregator] = _root_ide_package_.domain.value.Aggregator.ONEGAMEHUB
            it[AggregatorInfoTable.config] = mapOf(
                "gateway" to "staging.1gamehub.com",
                "salt" to "c2e160ea-32f4-47f3-a3ff-6b8c5dbe9131",
                "secret" to "02786922-6617-4333-aeda-c4863cf5ddb0",
                "partner" to "moonbet-staging"
            )
        }.value

        Result.success(
            _root_ide_package_.domain.model.AggregatorInfo(
                id = id,
                identity = identity,
                config = config,
                aggregator = type,
                active = true,
            )
        )
    }
}