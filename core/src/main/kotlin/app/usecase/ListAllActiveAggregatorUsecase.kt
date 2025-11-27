package app.usecase

import domain.aggregator.mapper.toAggregatorModel
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ListAllActiveAggregatorUsecase {
    suspend operator fun invoke(): List<AggregatorInfo> = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .map { it.toAggregatorModel() }
    }
}