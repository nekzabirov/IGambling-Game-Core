package com.nekzabirov.catalog.usecase

import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.catalog.model.AggregatorInfo
import com.nekzabirov.catalog.table.AggregatorInfoTable
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
            it[AggregatorInfoTable.config] = mapOf(
                "gateway" to "staging.1gamehub.com",
                "salt" to "c2e160ea-32f4-47f3-a3ff-6b8c5dbe9131",
                "secret" to "02786922-6617-4333-aeda-c4863cf5ddb0",
                "partner" to "moonbet-staging"
            )
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