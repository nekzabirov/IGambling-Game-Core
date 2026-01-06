package com.nekgamebling.infrastructure.handler.aggregator.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.aggregator.FindAggregatorQuery
import com.nekgamebling.application.port.inbound.aggregator.FindAggregatorResponse
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAggregatorQueryHandler : QueryHandler<FindAggregatorQuery, FindAggregatorResponse> {

    override suspend fun handle(query: FindAggregatorQuery): Result<FindAggregatorResponse> = newSuspendedTransaction {
        val row = AggregatorInfoTable
            .selectAll()
            .where { AggregatorInfoTable.identity eq query.identity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Aggregator", query.identity))

        Result.success(
            FindAggregatorResponse(
                aggregator = row.toAggregatorInfo()
            )
        )
    }
}
