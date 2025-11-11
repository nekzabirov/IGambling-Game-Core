package com.nekzabirov.catalog.usecase

import com.nekzabirov.catalog.mapper.toAggregatorModel
import com.nekzabirov.catalog.model.AggregatorInfo
import com.nekzabirov.catalog.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ListAllActiveAggregatorUsecase {
    suspend operator fun invoke(): List<AggregatorInfo> = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .map { it.toAggregatorModel() }
    }
}