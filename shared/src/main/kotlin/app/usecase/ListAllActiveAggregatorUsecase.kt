package app.usecase

import domain.mapper.toAggregatorModel
import domain.model.AggregatorInfo
import domain.table.AggregatorInfoTable
import domain.mapper.toAggregatorModel
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ListAllActiveAggregatorUsecase {
    suspend operator fun invoke(): List<domain.model.AggregatorInfo> = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .map { it.toAggregatorModel() }
    }
}